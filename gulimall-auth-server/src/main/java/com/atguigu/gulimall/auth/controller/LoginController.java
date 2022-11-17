package com.atguigu.gulimall.auth.controller;

import com.atguigu.common.exception.BizCodeEnume;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.auth.constant.AuthServerConstant;
import com.atguigu.gulimall.auth.feigin.ThirdPartFeignService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/16 22:21
 */
@Controller
@Slf4j
public class LoginController {

    @Autowired
    private ThirdPartFeignService thirdPartFeignService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @GetMapping({"/login.html", "/"})
    public String loginPage() {
        return "login";
    }

    /**
     * 发送验证码
     *
     * @param phone
     * @return
     */
    @RequestMapping("/sms/sendCode")
    @ResponseBody
    public R sendCode(@RequestParam("phone") String phone) {
        // TODO 1、接口防刷
        //发送之前先获取验证码
        String redisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if (!StringUtils.isEmpty(redisCode)) {
            long l = Long.parseLong(redisCode.split("_")[1]);
            if (System.currentTimeMillis() - l < 60 * 1000) {
                //1，2、60秒内不能发送
                return R.error(BizCodeEnume.SMS_CODE_EXCEPTION.getCode(), BizCodeEnume.SMS_CODE_EXCEPTION.getMsg());
            }
        }
        //2、存入redis 三分钟有效 key sms:code:phone
        String code = UUID.randomUUID().toString().substring(0, 5) + "_" + System.currentTimeMillis();
        //3、防止重复发送
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone, code, 3, TimeUnit.MINUTES);
        return thirdPartFeignService.sendCode(phone, code);
    }

}
