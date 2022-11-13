package com.atguigu.gulimall.search.vo;

import lombok.Data;

import java.util.List;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/12 21:08
 * <p>
 * 检索条件参数VO
 * 封装页面所有可以传递的查询条件
 * <p>
 * <p>
 * 1、全文检索：skuTitle-》keyword
 * 2、排序：saleCount（销量）、hotScore（热度分）、skuPrice（价格）
 * 3、过滤：hasStock、skuPrice区间、brandId、catalog3Id、attrs
 * 4、聚合：attrs
 * <p>
 * 完整查询路径:keyword=小米
 * &sort=saleCount_desc/asc&hasStock=0/1&skuPrice=400_1900&brandId=1&catalog3Id=1&at
 * trs=1_3G:4G:5G&attrs=2_骁龙845&attrs=4_高清屏
 */
@Data
public class SearchParam {


    private String keyword;//全文匹配关键字 全文检索


    private Long catalog3Id; //三级分类ID


    /**
     * sort=saleCount_desc/asc 销量
     * sort=hotScore_desc/asc 热度分
     * sort=skuPrice_desc/asc 价格
     */
    private String sort;

    /**
     * 过滤条件
     * hasStock（是否有货）、skuPrice区间、brandId、catalog3Id、attrs
     * hasStock=0/1
     * skuPrice=1_500/_500/500_
     *
     */

    private Integer hasStock;//是否有货

    private String skuPrice;//价格区间

    private List<Long> brandId;//品牌编号 可以多选

    private List<String> attrs;//按照属性进行筛选

    private Integer pageNum;//页码


}
