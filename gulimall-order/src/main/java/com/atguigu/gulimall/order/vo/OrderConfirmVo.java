package com.atguigu.gulimall.order.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.List;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/21 20:47
 */
public class OrderConfirmVo {


    /**
     * 会员收获地址列表
     **/
    @Getter
    @Setter
    List<MemberAddressVo> memberAddressVos;


    /**
     * 所有选中的购物项
     **/
    @Getter
    @Setter
    List<OrderItemVo> items;

    /**
     * 发票记录
     */

    /**
     * 优惠券（会员积分）
     **/
    @Getter
    @Setter
    private Integer integration;

    /**
     * 订单防重令牌
     */
    @Getter
    @Setter
    private String orderToken;

    /**
     * 订单总价
     */
    public BigDecimal getTotal() {
        BigDecimal bigDecimal = new BigDecimal("0");
        if (items != null) {
            for (OrderItemVo item : items) {
                //计算总价格当前数量乘以购物车每一项的价格
                BigDecimal price = item.getPrice();
                //获取总数
                Integer count = item.getCount();
                //相乘
                BigDecimal multiply = price.multiply(new BigDecimal(count));
                bigDecimal = bigDecimal.add(multiply);
            }
        }
        return bigDecimal;
    }

    /**
     * 实付款金额
     */
    public BigDecimal getPayPrice() {
        return getTotal();
    }
}
