package com.atguigu.gulimall.member.interceptor;

import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.vo.MemberResponseVo;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/19 20:26
 * 订单拦截器
 */
@Component
public class LoginUserInterceptor implements HandlerInterceptor {

    public static ThreadLocal<MemberResponseVo> threadLocal = new ThreadLocal<MemberResponseVo>();

    /**
     * 目标方法执行之前
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception MemberResponseVo member = (MemberResponseVo) session.getAttribute(AuthServerConstant.LOGIN_USER);
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        boolean match = new AntPathMatcher().match("/order/order/status/**", request.getRequestURI());
        if (match) {
            return true;
        }

        //获取登录的用户信息
        MemberResponseVo attribute = (MemberResponseVo) request.getSession().getAttribute(AuthServerConstant.LOGIN_USER);
        if (attribute != null) {
            //把登录后用户的信息放在ThreadLocal里面进行保存
            threadLocal.set(attribute);
            return true;
        } else {
            //未登录，返回登录页面
            response.setContentType("text/html;charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.println("<script>alert('请先进行登录，再进行后续操作！');location.href='http://auth.gulimall.com/login.html'</script>");
            // session.setAttribute("msg", "请先进行登录");
            // response.sendRedirect("http://auth.gulimall.com/login.html");
            return false;
        }
    }

}
