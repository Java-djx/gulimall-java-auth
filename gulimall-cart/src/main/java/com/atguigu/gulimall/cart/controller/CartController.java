package com.atguigu.gulimall.cart.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/19 18:38
 */
@Controller
public class CartController {

    @GetMapping("/cartList.html")
    public String goCartPage() {
        return "cartList";
    }
}
