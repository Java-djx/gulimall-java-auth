package com.atguigu.gulimall.cart.service;

import com.atguigu.gulimall.cart.vo.CartItem;

import java.util.concurrent.ExecutionException;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/19 20:16
 *  购物车
 */
public interface CartService {


    CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;
}
