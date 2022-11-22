package com.atguigu.gulimall.order.feifn;

import com.atguigu.gulimall.order.vo.OrderItemVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/21 21:11
 */
@FeignClient("gulimall-cart")
public interface CartFeignService {
    /*
     * 获取购物车的每一项
     * @return
     * @author djx
     * @deprecated: Talk is cheap,show me the code
     * @date 2022/11/21 20:59
     */
    @GetMapping("/currentUserCartItems")
    public List<OrderItemVo> getCurrentUserCartItems();
}
