package com.atguigu.gulimall.cart.service;

import com.atguigu.gulimall.cart.vo.Cart;
import com.atguigu.gulimall.cart.vo.CartItem;

import java.util.concurrent.ExecutionException;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/19 20:16
 * 购物车
 */
public interface CartService {


    /*
     * 新增购物车
     * @return
     * @author djx
     * @deprecated: Talk is cheap,show me the code
     * @date 2022/11/19 23:16
     */
    CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;

    /*
     * 获取购物车
     * @return
     * @author djx
     * @deprecated: Talk is cheap,show me the code
     * @date 2022/11/19 23:16
     */
    CartItem getCartItem(Long skuId);

     /*
      * 获取整个购物车
      * @return
      * @author djx
      * @deprecated: Talk is cheap,show me the code
      * @date 2022/11/19 23:48
      */
    Cart getCart() throws ExecutionException, InterruptedException;

}
