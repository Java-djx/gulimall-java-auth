package com.atguigu.gulimall.order.web;

import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.vo.OrderConfirmVo;
import com.atguigu.gulimall.order.vo.OrderSubmitVo;
import com.atguigu.gulimall.order.vo.SubmitOrderResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutionException;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/21 20:18
 */
@Controller
@Slf4j
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
    public String toTrade(Model model) throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = orderService.confirmOrder();
        model.addAttribute("orderConfirmData", confirmVo);
        return "confirm";
    }

     /*
      * 提交下单 
      * @return
      * @author djx
      * @deprecated: Talk is cheap,show me the code
      * @date 2022/11/24 12:06
      */
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo vo){
        //下单 创建订单，验证令牌，验价格
        SubmitOrderResponseVo responseVo= orderService.submitOrder(vo);
        log.info("订单提交的数据:{}",vo);
        //下单成功来到订单支付页面
        //下单失败重新返回订单页
        if (responseVo.getCode()==0){
            return "pay";
        }else{
            return "redirect:http://order.gulimall.com/toTrade";
        }
    }

}
