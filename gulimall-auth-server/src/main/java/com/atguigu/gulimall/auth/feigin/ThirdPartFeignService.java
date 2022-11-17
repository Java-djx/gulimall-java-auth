package com.atguigu.gulimall.auth.feigin;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/17 16:29
 */
@FeignClient("gulimall-third-party")
public interface ThirdPartFeignService {

    @RequestMapping("/sms/sendCode")
    public R sendCode(@RequestParam("phone") String phone, @RequestParam("code") String code);

}
