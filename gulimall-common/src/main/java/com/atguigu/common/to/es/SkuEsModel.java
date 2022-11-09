package com.atguigu.common.to.es;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/9 14:52
 * ES 商品数据模型
 */
@Data
public class SkuEsModel  {

    /**
     * skuId
     */
    private Long skuId;

    /**
     * spuId
     */
    private Long spuId;

    /**
     * skuTitle 标题
     */
    private String skuTitle;

    /**
     * sku价格
     */
    private BigDecimal skuPrice;

    /**
     * sku默认图片
     */
    private String skuImg;

    /**
     * 销售
     */
    private Long saleCount;

    /**
     * 是否存在库存
     */
    private Boolean hasStock;

    private Long hotScore;

    private Long brandId;

    private Long catalogId;

    private String brandName;

    private String brandImg;

    private String catalogName;

    private List<Attrs> attrs;

    @Data
    public static class Attrs{
        private Long attrId;

        private String attrName;

        private String attrValue;

    }
}
