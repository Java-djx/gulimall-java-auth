package com.atguigu.gulimall.ware.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/21 20:47
 *  订单购物项
 */
@Data
public class OrderItemVo {
    private Long skuId;
    private Boolean check = true;
    private String title;
    private String images;
    private List<String> skuAttr;
    private BigDecimal price;
    private Integer count;
    private BigDecimal totalPrice;
    /** 商品重量 **/
    private BigDecimal weight = new BigDecimal("0.085");


}
