package com.atguigu.common.to;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SkuReductionTo {

    /**
     * id
     * sku_id
     * 满几件
     * 打几折
     * 折后价
     * 是否叠加其他优惠[0-不可叠加，1-可叠加]
     */
    private Long skuId;
    private int fullCount;
    private BigDecimal discount;
    private int countStatus;
    private BigDecimal fullPrice;
    private BigDecimal reducePrice;
    private int priceStatus;
    private List<MemberPrice> memberPrice;
}
