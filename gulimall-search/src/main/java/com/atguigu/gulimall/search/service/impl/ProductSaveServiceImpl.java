package com.atguigu.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.gulimall.search.config.GuliMallElasticsearchConfig;
import com.atguigu.gulimall.search.constant.EsConstant;
import com.atguigu.gulimall.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/9 16:36
 */
@Service("productSaveService")
@Slf4j
public class ProductSaveServiceImpl implements ProductSaveService {

    @Resource
    private RestHighLevelClient restHighLevelClient;

    /**
     * 上架
     * 保存SKU数据
     *
     * @param esModels
     */
    @Override
    public boolean productStatusUp(List<SkuEsModel> esModels) throws IOException {

        log.info("es模型构造开始");
        //将模型保存到es中
        //1、给es中建立索引 product 和映射关系
        //2、给ES索引中保存数据
        //BulkRequest bulkRequest, RequestOptions options 批量保存
        BulkRequest bulkRequest = new BulkRequest();
        //批量保存
        for (SkuEsModel esModel : esModels) {
            IndexRequest indexRequest = new IndexRequest(EsConstant.PRODUCT_INDEX);
            indexRequest.id(esModel.getSkuId().toString());
            String toJSONString = JSON.toJSONString(esModel);
            indexRequest.source(toJSONString, XContentType.JSON);
            bulkRequest.add(indexRequest);
        }
        //响应结构
        BulkResponse bulk = restHighLevelClient.bulk(bulkRequest, GuliMallElasticsearchConfig.COMMON_OPTIONS);
        //TODO 如果批量错误处理错误
        boolean b = bulk.hasFailures();
        List<String> list = Arrays.stream(bulk.getItems()).map(item -> {
            return item.getId();
        }).collect(Collectors.toList());
        log.error("商品上架错误，错误的数据:{}", list);


        return b;

    }
}
