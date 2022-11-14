package com.atguigu.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.search.config.GuliMallElasticsearchConfig;
import com.atguigu.gulimall.search.constant.EsConstant;
import com.atguigu.gulimall.search.feigin.ProductFeignService;
import com.atguigu.gulimall.search.service.MallSearchService;
import com.atguigu.gulimall.search.vo.AttrResponseVo;
import com.atguigu.gulimall.search.vo.BrandVo;
import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/12 21:10
 */
@Service
@Slf4j
public class MallSearchServiceImpl implements MallSearchService {

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    private ProductFeignService productFeignService;

    @Override
    public SearchResult search(SearchParam searchParam) {
        //1、动态构造查询条件
        SearchResult result = null;
        //2、准备检索请求
        SearchRequest searchRequest = buildSearchRequest(searchParam);
        try {
            //3、执行检索请求
            SearchResponse response = client.search(searchRequest, GuliMallElasticsearchConfig.COMMON_OPTIONS);
            //4、分析响应数据封装成需要的结果
            result = buildSearchResult(response, searchParam);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        log.info("检索返回的数据:{}", result);
        //构造面包屑导航
        if (searchParam.getAttrs() != null && searchParam.getAttrs().size() > 0) {

            //设置面包的内容
            SearchResult finalResult = result;
            List<SearchResult.NavVo> navVos = searchParam.getAttrs().stream().map(attr -> {
                //分析每一个attrs
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                //attrs=1_3G:4G:5G
                String[] att = attr.split("_");
                navVo.setNavValue(att[1]);
                R r = productFeignService.attrInfo(Long.parseLong(att[0]));
                finalResult.getAttrIds().add(Long.parseLong(att[0]));

                if (r.getCode() == 0) {
                    AttrResponseVo date = r.getDate("attr", new TypeReference<AttrResponseVo>() {
                    });
                    navVo.setNavName(date.getAttrName());
                } else {
                    navVo.setNavName(att[0]);
                }
                String replace = replaceQueryString(searchParam, attr, "attrs");
                navVo.setLink("http://search.gulimall.com/list.html?" + replace);
                return navVo;
            }).collect(Collectors.toList());
            result.setNavs(navVos);
            if (searchParam.getBrandId() != null && searchParam.getBrandId().size() > 0) {
                List<SearchResult.NavVo> navs = result.getNavs();
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                navVo.setNavName("品牌");
                //TODO 远程查询所有品牌
                R r = productFeignService.BrandsInfos(searchParam.getBrandId());
                if (r.getCode() == 0) {
                    List<BrandVo> date = r.getDate(new TypeReference<List<BrandVo>>() {
                    });
                    StringBuffer stringBuffer = new StringBuffer();
                    String replace = "";
                    for (BrandVo brandVo : date) {
                        stringBuffer.append(brandVo.getBrandName() + ";");
                        replace = replaceQueryString(searchParam, brandVo.getBrandId() + "", "brandId");
                    }
                    navVo.setNavValue(stringBuffer.toString());
                    navVo.setLink("http://search.gulimall.com/list.html?" + replace);
                }
                navs.add(navVo);
            }
        }
        return result;
    }

    private String replaceQueryString(SearchParam searchParam, String value, String key) {
        String encode = null;
        try {
            encode = URLEncoder.encode(value, "UTF-8");
            encode.replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        ;
        //2、取消了面包屑以后我们要跳转到什么地方，讲请求地址的url制空
        return searchParam.get_queryString().replace("&" + key + "=" + encode, "");
    }

    /**
     * 构建结果数据
     *
     * @param response
     * @return
     */
    private SearchResult buildSearchResult(SearchResponse response, SearchParam param) {

        SearchResult searchResult = new SearchResult();

        SearchHits hits = response.getHits();
        List<SkuEsModel> esModels = new ArrayList<SkuEsModel>();
        //命中的每一条记录
        if (hits.getHits() != null && hits.getHits().length > 0) {
            for (SearchHit hit : hits.getHits()) {
                //获取命中的数据
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel esModel = JSON.parseObject(sourceAsString, SkuEsModel.class);
                //如果检索条件不等于空就设置文本的高亮
                if (!StringUtils.isEmpty(param.getKeyword())) {
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String string = skuTitle.getFragments()[0].string();
                    esModel.setSkuTitle(string);
                }
                esModels.add(esModel);
            }
        }
        //1、返回的所有查询的商品
        searchResult.setProducts(esModels);


        //2、当前商品涉及的所有属性消息
        List<SearchResult.AttrVo> attrVos = new ArrayList<SearchResult.AttrVo>();
        ParsedNested attr_agg = response.getAggregations().get("attr_agg");
        //得到属性ID的聚合
        ParsedLongTerms attr_id_agg = attr_agg.getAggregations().get("attr_id_agg");
        for (Terms.Bucket bucket : attr_id_agg.getBuckets()) {
            //1、得到属性的id
            long attrId = bucket.getKeyAsNumber().longValue();
            //2、得到属性的名字
            String attrName = ((ParsedStringTerms) bucket.getAggregations().get("attr_name_agg")).getBuckets().get(0).getKeyAsString();
            //3、得到属性的所有值
            List<String> attrValues = ((ParsedStringTerms) bucket.getAggregations().get("attr_value_agg"))
                    .getBuckets().stream()
                    .map(item -> {
                        String keyAsString = item.getKeyAsString();
                        return keyAsString;
                    }).collect(Collectors.toList());
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo(attrId, attrName, attrValues);

            attrVos.add(attrVo);
        }
        searchResult.setAttrs(attrVos);
        //3、当前商品涉及的所有品牌消息
        //获取品牌的聚合 从聚合中抽取数据 抽取子数据
        ParsedLongTerms brand_agg = response.getAggregations().get("brand_agg");
        List<SearchResult.BrandVo> brandVos = new ArrayList<SearchResult.BrandVo>();
        List<? extends Terms.Bucket> brandBuckets = brand_agg.getBuckets();
        for (Terms.Bucket bucket : brandBuckets) {
            //从品牌聚合中获取品牌的一切消息
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            //得到品牌的ID
            brandVo.setBrandId(bucket.getKeyAsNumber().longValue());
            //得到并设置品牌的名字
            ParsedStringTerms brand_name_agg = bucket.getAggregations().get("brand_name_agg");
            brandVo.setBrandName(brand_name_agg.getBuckets().get(0).getKeyAsString());
            //得到并设置品牌的图片
            ParsedStringTerms brand_img_agg = bucket.getAggregations().get("brand_img_agg");
            brandVo.setBrandImg(brand_img_agg.getBuckets().get(0).getKeyAsString());
            brandVos.add(brandVo);
        }
        searchResult.setBrands(brandVos);
        //4、当前商品涉及的所有分类消息
        ParsedLongTerms catalog_agg = response.getAggregations().get("catalog_agg");
        List<SearchResult.CatalogVo> catalogVos = new ArrayList<SearchResult.CatalogVo>();
        List<? extends Terms.Bucket> buckets = catalog_agg.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            //分类Id
            catalogVo.setCatalogId(Long.valueOf(bucket.getKeyAsString()));
            // 从分类聚合中获取子聚合分类的名字
            ParsedStringTerms catalog_name_agg = bucket.getAggregations().get("catalog_name_agg");
            String CatalogName = catalog_name_agg.getBuckets().get(0).getKeyAsString();
            catalogVo.setCatalogName(CatalogName);
            catalogVos.add(catalogVo);
        }
        searchResult.setCatalogs(catalogVos);
        //5、当前商品涉及的所有分页消息
        //当前页码
        searchResult.setPageNum(param.getPageNum());
        //总记录数
        long total = hits.getTotalHits().value;
        searchResult.setTotal(total);
        //总页码 --- 计算 总记录数%每页展示的数据==0?总记录数/每页展示的数据:总记录数/每页展示的数据+1
        Integer totalPages =
                (int) total % EsConstant.PRODUCT_SIZE == 0 ? (int) total / EsConstant.PRODUCT_SIZE :
                        ((int) total / EsConstant.PRODUCT_SIZE + 1);
        searchResult.setTotalPages(totalPages);

        //页码
        List<Integer> pageNavs = new ArrayList<>();
        for (Integer i = 1; i <= totalPages; i++) {
            pageNavs.add(i);
        }
        searchResult.setPageNavs(pageNavs);
        return searchResult;
    }


    /**
     * 准备检索请求
     * 模糊匹配、过滤（属性，分类，品牌，价格区间，库存）、排序、分页、亮高、聚合分析
     *
     * @return SearchRequest
     */
    private SearchRequest buildSearchRequest(SearchParam param) {
        //构建DSL语句
        SearchSourceBuilder source = new SearchSourceBuilder();
        /**
         * 过滤（属性，分类，品牌，价格区间，库存）
         */
        //1、构造 bool-query
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        //1.1 构造 must 模糊匹配
        if (!StringUtils.isEmpty(param.getKeyword())) {
            boolQuery.must(QueryBuilders.matchQuery("skuTitle", param.getKeyword()));
        }
        //1.2 bool-filter 按照三级分类id查询
        if (!StringUtils.isEmpty(param.getCatalog3Id())) {
            boolQuery.filter(QueryBuilders.termQuery("catalogId", param.getCatalog3Id()));
        }
        //1.2 bool-filter 按照 品牌 id查询
        if (param.getBrandId() != null && param.getBrandId().size() > 0) {
            boolQuery.filter(QueryBuilders.termsQuery("brandId", param.getBrandId()));
        }
        //1.2 bool-filter 按照 属性 查询
        if (param.getAttrs() != null && param.getAttrs().size() > 0) {
            for (String attrStr : param.getAttrs()) {
                BoolQueryBuilder nestedBoolBuilder = QueryBuilders.boolQuery();
                //1_3G:4G:5G
                String[] s = attrStr.split("_");
                String attrId = s[0];
                //3G:4G:5G
                String[] attrValues = s[1].split(":");
                nestedBoolBuilder.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                nestedBoolBuilder.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));
                //每一个都必须生成一个切入的查询条件
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", nestedBoolBuilder, ScoreMode.None);
                boolQuery.filter(nestedQuery);
            }
        }
        if (param.getHasStock() != null) {
            //1.2 bool-filter 按照 是否存在库存查询
            boolQuery.filter(QueryBuilders.termQuery("hasStock", param.getHasStock() == 1));
        }
        //1.2 bool-filter 按照 价格区间 查询
        if (!StringUtils.isEmpty(param.getSkuPrice())) {
            //skuPrice=1_500/_500/500_
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
            String[] s = param.getSkuPrice().split("_");
            if (s.length == 2) {
                //区间
                rangeQuery.gte(s[0]).lte(s[1]);
            } else if (s.length == 1) {
                if (param.getSkuPrice().startsWith("_")) {
                    //_500 小于区间
                    rangeQuery.lte(s[0]);
                }
                if (param.getSkuPrice().endsWith("_")) {
                    //500_ 大于区间
                    rangeQuery.gte(s[0]);
                }
            }
            boolQuery.filter(rangeQuery);
        }
        source.query(boolQuery);
        /**
         * 2.1 排序
         */
        if (!StringUtils.isEmpty(param.getSort())) {
            /**
             * sort=saleCount_desc/asc 销量
             * sort=hotScore_desc/asc 热度分
             * sort=skuPrice_desc/asc 价格
             */
            String[] sortStr = param.getSort().split("_");
            String sortName = sortStr[0];
            String sortOrders = sortStr[1];
            //根据拆分的排序条件
            SortOrder sortOrder = sortOrders.equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC;
            source.sort(sortName, sortOrder);
        }
        //2.2分页 pageSize:5
        //pageNum:1 from:0 size:5
        //pageNum:2 from:5 size:5
        // （pageNum-1）* size
        source.from(
                (param.getPageNum() - 1) * EsConstant.PRODUCT_SIZE
        );
        source.size(EsConstant.PRODUCT_SIZE);
        //2.3 亮高
        if (!StringUtils.isEmpty(param.getKeyword())) {
            HighlightBuilder builder = new HighlightBuilder();
            builder.field("skuTitle");
            builder.preTags("<b style='color:red'>");
            builder.postTags("</b>");
            source.highlighter(builder);
        }
        /**
         * 聚合分析
         */
        //3.1构造聚合分析
        // 3.2 品牌聚合
        TermsAggregationBuilder brandAgg = AggregationBuilders.terms("brand_agg");
        brandAgg.field("brandId").size(100);
        // 3.2.1 品牌聚合的子聚合 品牌名称
        TermsAggregationBuilder brandNameAgg = AggregationBuilders.terms("brand_name_agg").field("brandName").size(1);
        brandAgg.subAggregation(brandNameAgg);
        // 3.2.1 品牌聚合的子聚合 品牌图片
        TermsAggregationBuilder brandImgAgg = AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1);
        brandAgg.subAggregation(brandImgAgg);
        source.aggregation(brandAgg);
        // 3.2 分类聚合
        TermsAggregationBuilder catalogAgg = AggregationBuilders.terms("catalog_agg");
        catalogAgg.field("catalogId").size(100);
        // 3.2.1 分类聚合的子聚合 分类名字
        TermsAggregationBuilder catalogNameAgg = AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1);
        catalogAgg.subAggregation(catalogNameAgg);
        source.aggregation(catalogAgg);

        //3.3 属性聚合
        NestedAggregationBuilder nested = AggregationBuilders.nested("attr_agg", "attrs");
        // 内嵌属性聚合的子聚合 聚合AttrId
        TermsAggregationBuilder attrIdAgg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId").size(100);
        nested.subAggregation(attrIdAgg);
        // 属性聚合的子聚合 属性名字
        TermsAggregationBuilder attrNameAgg = AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1);
        attrIdAgg.subAggregation(attrNameAgg);
        // 属性聚合的子聚合 属性值
        TermsAggregationBuilder attValueAgg = AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(100);
        attrIdAgg.subAggregation(attValueAgg);
        source.aggregation(nested);
        //返回检索请求
        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, source);
        log.info("构建的DSL语句:{}", source.toString());

        return searchRequest;
    }
}
