package com.atguigu.gulimall.cart.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/19 19:59
 * 整个购物车
 * 需要计算的属性必须重写他的get方法，保存每一项都需要计算
 */
public class Cart {

    private List<CartItem> items;

    private Integer countNum;//商品数量

    private Integer countType;//商品类型属性

    private BigDecimal totalAmount;//商品总价

    private BigDecimal reduce = new BigDecimal(0);//减免价格


    public List<CartItem> getCartItems() {
        return items;
    }

    public void setCartItems(List<CartItem> items) {
        this.items = items;
    }

    public Integer getCountNum() {
        int count = 0;
        //计算商品总数量
        if (items != null && items.size() > 0) {
            for (CartItem item : items) {
                count += item.getCount();
            }
        }
        return count;
    }


    public Integer getCountType() {
        int count = 0;
        //计算商品总类型
        if (items != null && items.size() > 0) {
            for (CartItem item : items) {
                count += 1;
            }
        }
        return count;
    }


    /**
     * 计算付款总价 减去优惠价
     *
     * @return
     */
    public BigDecimal getTotalAmount() {
        BigDecimal bigDecimal = new BigDecimal("0");
        //计算购物项总价
        if (items != null && items.size() > 0) {
            for (CartItem item : items) {
                BigDecimal totalPrice = item.getTotalPrice();
                bigDecimal = bigDecimal.add(totalPrice);
            }
        }
        //减去优惠券总价
        BigDecimal subtract = bigDecimal.subtract(getReduce());
        return subtract;
    }


    public BigDecimal getReduce() {
        return reduce;
    }

    public void setReduce(BigDecimal reduce) {
        this.reduce = reduce;
    }
}
