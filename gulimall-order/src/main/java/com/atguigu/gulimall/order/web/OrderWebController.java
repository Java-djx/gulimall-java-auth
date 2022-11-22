package com.atguigu.gulimall.order.web;

import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.vo.OrderConfirmVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/21 20:18
 */
@Controller
public class OrderWebController {


    @Autowired
    private OrderService orderService;

    /*
     * 跳转到订单页面查询出订单的消息
     * @return
     * @author djx
     * @deprecated: Talk is cheap,show me the code
     * @date 2022/11/21 20:18
     */
    @GetMapping("/toTrade")
    public String toTrade(Model model) {

        OrderConfirmVo confirmVo = orderService.confirmOrder();

        model.addAttribute("orderConfirmData", confirmVo);

        return "confirm";
    }

}
