package com.atguigu.gulimall.auth.controller;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.exception.BizCodeEnume;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberResponseVo;
import com.atguigu.gulimall.auth.constant.AuthServerConstant;
import com.atguigu.gulimall.auth.feigin.MemberFeignService;
import com.atguigu.gulimall.auth.feigin.ThirdPartFeignService;
import com.atguigu.gulimall.auth.vo.UserLoginVo;
import com.atguigu.gulimall.auth.vo.UserRegisterVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.atguigu.common.constant.AuthServerConstant.LOGIN_USER;

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


    @Autowired
    private MemberFeignService memberFeignService;

    @GetMapping(value = "/login.html")
    public String loginPage(HttpSession session) {
        //从session先取出来用户的信息，判断用户是否已经登录过了
        Object attribute = session.getAttribute(LOGIN_USER);
        //如果用户没登录那就跳转到登录页面
        if (attribute == null) {
            return "login";
        } else {
            return "redirect:http://gulimall.com";
        }
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
        int code = (int) ((Math.random() * 9 + 1) * 100000);
        String codeNum = String.valueOf(code);
        String substring = code + "_" + System.currentTimeMillis();
        //3、防止重复发送
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone, substring, 5, TimeUnit.MINUTES);
        return thirdPartFeignService.sendCode(phone, String.valueOf(code));
    }

    @PostMapping(value = "/register")
    public String register(@Valid UserRegisterVo vos, BindingResult result,
                           RedirectAttributes attributes) {
        //如果有错误回到注册页面
        if (result.hasErrors()) {
            Map<String, String> errors = result.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            attributes.addFlashAttribute("errors", errors);
            //效验出错回到注册页面
            return "redirect:http://auth.gulimall.com/reg.html";
        }
        //1.校验验证码
        String redisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vos.getPhone());
        if (!StringUtils.isEmpty(redisCode)) {
            // 拆分验证码
            String s = redisCode.split("_")[0];
            if (vos.getCode().equalsIgnoreCase(s)) {
                //删除令牌机制 令牌机制
                redisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vos.getPhone());
                //真正注册调用远程服务
                R r = memberFeignService.regist(vos);
                if (r.getCode() == 0) {
                    //成功
                    return "redirect:http://auth.gulimall.com/login.html";
                } else {
                    //失败
                    String date = r.getData("msg", new TypeReference<String>() {
                    });
                    Map<String, String> errors = new HashMap<>();
                    errors.put("msg", date);
                    attributes.addFlashAttribute("errors", errors);
                    return "redirect:http://auth.gulimall.com/reg.html";
                }
            } else {
                Map<String, String> errors = new HashMap<>();
                errors.put("code", "验证码错误！");
                attributes.addFlashAttribute("errors", errors);
                return "redirect:http://auth.gulimall.com/reg.html";
            }
        } else {
            Map<String, String> errors = new HashMap<>();
            errors.put("code", "验证码已过期请重新发送！");
            attributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/reg.html";
        }
    }


    /**
     * 登录
     *
     * @param vo
     * @return
     */
    @PostMapping("/login")
    public String login(UserLoginVo vo, RedirectAttributes attributes, HttpSession session) {

        R r = memberFeignService.login(vo);
        if (r.getCode() == 0) {
            MemberResponseVo data = r.getData("data", new TypeReference<MemberResponseVo>() {
            });
            log.info("登录成功：用户信息：{}", data.toString());
            session.setAttribute(LOGIN_USER, data);
            //1、第一次使用session，命令浏览器保存卡号，JSESSIONID这个cookie
            //以后浏览器访问哪个网站就会带上这个网站的cookie
            //TODO 1、默认发的令牌。当前域（解决子域session共享问题）
            //TODO 2、使用JSON的序列化方式来序列化对象到Redis中
            //成功
            return "redirect:http://gulimall.com/";
        } else {
            //失败
            String date = r.getData("msg", new TypeReference<String>() {
            });
            Map<String, String> errors = new HashMap<>();
            errors.put("msg", date);
            attributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }
    }

}
