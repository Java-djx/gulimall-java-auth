package com.atguigu.gulimall.member.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/29 14:38
 */
@Controller
public class MemberWebConroller {

    @GetMapping("/memberOrder.html")
    public String memberOrderPage() {

        //查询当前登录用户的所有数据

        return "orderList";
    }

}
