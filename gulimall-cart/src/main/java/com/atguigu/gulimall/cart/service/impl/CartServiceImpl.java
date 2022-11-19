package com.atguigu.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.CartConstant;
import com.atguigu.common.to.SkuInfoEntityTo;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.cart.feign.ProductFeignService;
import com.atguigu.gulimall.cart.interceptor.CartInterceptor;
import com.atguigu.gulimall.cart.service.CartService;
import com.atguigu.gulimall.cart.vo.CartItem;
import com.atguigu.gulimall.cart.vo.UserInfoTo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/19 20:17
 */
@Slf4j
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private ThreadPoolExecutor executor;

    @Override
    public CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();

        /**
         * 添加新商品
         */

        //首先去redis中查询数据
        String redisCart = (String) cartOps.get(skuId.toString());
        if (StringUtils.isEmpty(redisCart)) {
            CartItem cartItem = new CartItem();
            //使用异步线程池
            //1、远程调用查询商品详情
            CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
                R r = productFeignService.getSkuInfo(skuId);
                if (r.getCode() == 0) {
                    //2、封装商品消息
                    SkuInfoEntityTo skuInfo = r.getData("skuInfo", new TypeReference<SkuInfoEntityTo>() {
                    });
                    cartItem.setCount(num);
                    cartItem.setImages(skuInfo.getSkuDefaultImg());
                    cartItem.setTitle(skuInfo.getSkuTitle());
                    cartItem.setSkuId(skuInfo.getSkuId());
                    cartItem.setPrice(skuInfo.getPrice());
                    cartItem.setCheck(true);
                }
            }, executor);
            CompletableFuture<Void> getSkuAttrValueFuture = CompletableFuture.runAsync(() -> {
                //3、远程查询商品的属性组合
                List<String> values = productFeignService.getSkuSaleAttrValues(skuId);
                cartItem.setSkuAttr(values);
            }, executor);
            CompletableFuture.allOf(getSkuInfoTask, getSkuAttrValueFuture).get();
            //2、添加购物车
            String toJSONString = JSON.toJSONString(cartItem);
            cartOps.put(skuId, toJSONString);

            return cartItem;
        } else {
            CartItem cartItem = new CartItem();
            //购物车有商品
            cartItem = JSON.parseObject(redisCart, CartItem.class);
            cartItem.setCount(cartItem.getCount() + num);
            //修改购物数量
            String toJSONString = JSON.toJSONString(cartItem);
            cartOps.put(skuId, toJSONString);
            return cartItem;
        }

    }

    /**
     * 首先获取我们要操作的购物车
     *
     * @return
     */
    private BoundHashOperations<String, Object, Object> getCartOps() {
        //1、目标方法快速得到数据
        UserInfoTo userInfoTo = CartInterceptor.toThreadLocal.get();
        //2、判断当前是临时令牌还是用户登录令牌
        String cartKey = "";
        if (userInfoTo.getUserId() != null) {
            //gulimall:cart:1
            cartKey = CartConstant.CART_PREFIX + userInfoTo.getUserId().toString();
        } else {
            //gulimall:cart:uuid
            cartKey = CartConstant.CART_PREFIX + userInfoTo.getUserKey();
        }
        //操作购物车 添加购物车
        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(cartKey);

        return operations;
    }

}
