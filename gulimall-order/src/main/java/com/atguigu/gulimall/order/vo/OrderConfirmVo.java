package com.atguigu.gulimall.order.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
    List<MemberAddressVo> address;


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

    @Getter
    @Setter
    private Map<Long, Boolean> stocks;

    /**
     * 订单防重令牌
     */
    @Getter
    @Setter
    private String orderToken;




    public Integer getCount() {
        Integer count = 0;
        if (items != null && items.size() > 0) {
            for (OrderItemVo item : items) {
                count += item.getCount();
            }
        }
        return count;
    }

    /**
     * 订单总额
     **/
    //BigDecimal total;
    //计算订单总额
    public BigDecimal getTotal() {
        BigDecimal totalNum = BigDecimal.ZERO;
        if (items != null && items.size() > 0) {
            for (OrderItemVo item : items) {
                //计算当前商品的总价格
                BigDecimal itemPrice = item.getPrice().multiply(new BigDecimal(item.getCount().toString()));
                //再计算全部商品的总价格
                totalNum = totalNum.add(itemPrice);
            }
        }
        return totalNum;
    }

    /**
     * 实付款金额
     */
    public BigDecimal getPayPrice() {
        return getTotal();
    }
}
