package com.atguigu.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/24 12:01
 */
@Data
public class OrderSubmitVo {

    private Long addrId;//收货地址Id
    private Integer payType;//支付类型
    //无需提交购买的商品再去购物车查询一遍
    //优惠,发票

    private String orderToken;//防重复令牌

    private BigDecimal payPrice;//应付价格

    private String note;//订单备注
    //用户相关消息直接去session取出


}
