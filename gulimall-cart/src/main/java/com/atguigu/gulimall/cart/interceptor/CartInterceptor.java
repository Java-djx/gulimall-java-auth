package com.atguigu.gulimall.cart.interceptor;

import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.constant.CartConstant;
import com.atguigu.common.vo.MemberResponseVo;
import com.atguigu.gulimall.cart.vo.UserInfoTo;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.UUID;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/19 20:26
 * <p>
 * 购物车拦截器
 *   拦截构造临时用户
 *   临时用户保存的购物车合并到登录后的购物车
 *    通过一个临时key 登录未登录都存在
 *    未登录的时候首先放在以临时key做为标识 键是临时 key
 *    登录之后
 *    根据临时key的键获取购物车 合并一个新的购物车 键是用户id
 */
@Component
public class CartInterceptor implements HandlerInterceptor {

    public static ThreadLocal<UserInfoTo> toThreadLocal = new ThreadLocal<UserInfoTo>();

    /**
     * 目标方法执行之前
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();
        MemberResponseVo member = (MemberResponseVo) session.getAttribute(AuthServerConstant.LOGIN_USER);
        UserInfoTo userInfoTo = new UserInfoTo();
        //用户登录
        if (member != null) {
            userInfoTo.setUserId(member.getId());
        }
        //临时用户处理
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            //循环页面存放的用户临时key
            for (Cookie cookie : cookies) {
                String name = cookie.getName();
                // 对比user-key
                if (name.equals(CartConstant.TEMP_USER_COOKIE_NAME)) {
                    //临时用户存放数据页面临时key的值 设置临时用户
                    userInfoTo.setUserKey(cookie.getValue());
                    userInfoTo.setTempUser(true);
                }
            }
        }
        //分配一个临时用户 一定要分配一个
        if (StringUtils.isEmpty(userInfoTo.getUserKey())) {
            String uuid = UUID.randomUUID().toString();
            userInfoTo.setUserKey(uuid);
        }
        //放行之前存放数据
        toThreadLocal.set(userInfoTo);
        return true;
    }

    /**
     * 业务执行只有 让游览器保存临时令牌
     *
     * @param request
     * @param response
     * @param handler
     * @param modelAndView
     * @throws Exception
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        UserInfoTo userInfoTo = toThreadLocal.get();
        if (!userInfoTo.getTempUser()) {
            //仅仅创建一次不会永远延迟 一个月的过期时间
            Cookie cookie = new Cookie(CartConstant.TEMP_USER_COOKIE_NAME, userInfoTo.getUserKey());
            cookie.setMaxAge(CartConstant.TEMP_USER_COOKIE_TIMOUR);
            cookie.setDomain("gulimall.com");
            response.addCookie(cookie);
        }
    }
}
