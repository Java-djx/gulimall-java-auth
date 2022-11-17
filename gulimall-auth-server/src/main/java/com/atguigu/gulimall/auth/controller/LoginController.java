package com.atguigu.gulimall.auth.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/16 22:21
 */
@Controller
@Slf4j
public class LoginController {

    @GetMapping({"/login.html","/"})
    public String loginPage(){
        return "login";
    }

}
