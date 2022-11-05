package com.atguigu.gulimall.member.fegin;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.Arrays;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/2 17:05
 * @FeignClient("") 远程调用客服端
 * 注解加接口的形式
 * 声明式远程调用
 */
@FeignClient("gulimall-coupon")
public interface CouponFeignService {

    @RequestMapping("/coupon/coupon/member/list")
    public R membercoupons();

}
