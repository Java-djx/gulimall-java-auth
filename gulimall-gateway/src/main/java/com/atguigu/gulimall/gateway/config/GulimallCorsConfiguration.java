package com.atguigu.gulimall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/3 13:20
 */
@Configuration
public class GulimallCorsConfiguration {


    @Bean
    public CorsWebFilter corsWebFilter(){
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        //跨域对象
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        //设置请求头
        corsConfiguration.addAllowedHeader("*");
        //请求方式
        corsConfiguration.addAllowedMethod("*");
        //请求来源
        corsConfiguration.addAllowedOrigin("*");
        //是否允许携带cookie
        corsConfiguration.setAllowCredentials(true);
        //拦截跨域的资源
        source.registerCorsConfiguration("/**",corsConfiguration);
        return new CorsWebFilter(source);
    }

}
