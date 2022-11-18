package com.atguigu.gulimall.auth.feigin;

import com.atguigu.common.utils.R;
import com.atguigu.gulimall.auth.vo.UserLoginVo;
import com.atguigu.gulimall.auth.vo.UserRegisterVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/17 20:28
 * 调用会员服务
 */
@FeignClient("gulimall-member")
public interface MemberFeignService {


    /**
     * 注册
     *
     * @param vo
     * @return
     */
    @RequestMapping("/member/member/regist")
    public R regist(@RequestBody UserRegisterVo vo);

    /**
     * 登录
     *
     * @param vo
     * @return
     */
    @RequestMapping("/member/member/login")
    public R login(@RequestBody UserLoginVo vo);
}
