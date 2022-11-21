package com.atguigu.gulimall.order.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/21 20:18
 */
@Controller
public class OrderWebController {

    /*
     * 结算
     * @return
     * @author djx
     * @deprecated: Talk is cheap,show me the code
     * @date 2022/11/21 20:18
     */
    @GetMapping("/toTrade")
    public String toTrade() {
        return "confirm";
    }

}
