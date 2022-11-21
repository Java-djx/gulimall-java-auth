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


    /*
     清空购物车数据
     * @return
     * @author djx
     * @deprecated: Talk is cheap,show me the code
     * @date 2022/11/21 13:46
     */
    public void clearCart(String cartKey);

     /*
      * 勾选购物项
      * @return
      * @author djx
      * @deprecated: Talk is cheap,show me the code
      * @date 2022/11/21 14:36
      */
    void checkItem(Long skuId, Integer check);

     /*
      * 修改购物项数量
      * @return
      * @author djx
      * @deprecated: Talk is cheap,show me the code
      * @date 2022/11/21 14:46
      */
    void changeItemCount(Long skuId, Integer num);

     /*
      * 删除购物项
      * @return
      * @author djx
      * @deprecated: Talk is cheap,show me the code
      * @date 2022/11/21 15:06
      */
    void deleteItem(Long skuId);

}

