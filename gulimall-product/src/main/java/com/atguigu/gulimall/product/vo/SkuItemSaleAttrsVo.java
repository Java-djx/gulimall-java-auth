package com.atguigu.gulimall.product.vo;

import lombok.Data;

import java.util.List;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/15 21:05
 */
@Data
public class SkuItemSaleAttrsVo {

    /**
     * 属性id
     */
    private Long attrId;
    /**
     * 属性名
     */
    private String attrName;
    /**
     * 属性对应的值
     */
    private List<AttrValueWithSkuIdVo> attrValues;

}
