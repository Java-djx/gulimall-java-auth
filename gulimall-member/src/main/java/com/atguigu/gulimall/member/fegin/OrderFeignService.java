package com.atguigu.gulimall.member.fegin;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/29 15:33
 */
@FeignClient("gulimall-order")
public interface OrderFeignService {

    @RequestMapping("/order/order/listWithItem")
    public R listWithItem(@RequestBody Map<String, Object> params);
}
