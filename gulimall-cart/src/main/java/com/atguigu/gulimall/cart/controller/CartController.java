package com.atguigu.gulimall.cart.controller;

import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.gulimall.cart.interceptor.CartInterceptor;
import com.atguigu.gulimall.cart.service.CartService;
import com.atguigu.gulimall.cart.vo.CartItem;
import com.atguigu.gulimall.cart.vo.UserInfoTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.concurrent.ExecutionException;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/19 18:38
 */
@Controller
@Slf4j
public class CartController {


    @Autowired
    private CartService cartService;

    /**
     * 跳转到购物车页面
     * 1、 游览器有一个user-key 标识用户的身份一个月过期
     * 2.如果第一次使用京东购物车都会给一个临时身份 以后访问都会带上
     * 3.如果登录session里面有
     * 4.没登录按照user-key操作
     * 5.如果没有临时用户创建一个临时用户
     *
     * @return
     */
    @GetMapping("/cartList.html")
    public String cartListPage(HttpSession session) {

        //1.目标方法快速得到数据
        UserInfoTo userInfoTo = CartInterceptor.toThreadLocal.get();

        log.info("获取到线程中内存用户{}", userInfoTo);

        return "cartList";
    }


    /*
     * @return
     * @author djx
     * @deprecated: Talk is cheap,show me the code
     * @date 2022/11/19 21:49
     */
    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num, Model model) {

        CartItem cartItem = null;
        try {
            cartItem = cartService.addToCart(skuId, num);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        model.addAttribute("item", cartItem);

        return "success";
    }

}
