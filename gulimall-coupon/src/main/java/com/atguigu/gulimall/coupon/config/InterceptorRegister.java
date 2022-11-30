package com.atguigu.gulimall.coupon.config;


import ch.qos.logback.classic.pattern.DateConverter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.text.DateFormat;
import java.util.List;
import java.util.TimeZone;


/**
 * @author Nnmy
 * @version 1.0
 * 继承WebMvcConfigurationSupport 类后, 会覆盖 @EnableAutoConfiguration 关于
 * WebMvcAutoConfiguration 的配置 (默认配置会失效,需要自己配置)
 */
@Slf4j
@Configuration
public class InterceptorRegister extends WebMvcConfigurationSupport {
    //接收页面参数使用DateConverter自动转化
    @Override
    protected void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new GlobalFormDateConvert());
    }

    /**
     * 使用此方法, 以下 spring-boot: jackson时间格式化 配置 将会失效
     * spring.jackson.time-zone=GMT+8
     * spring.jackson.date-format=yyyy-MM-dd HH:mm:ss
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        System.out.println("进入自动以自从转换");
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        ObjectMapper objectMapper = converter.getObjectMapper();
        // 生成JSON时,将所有Long转换成String
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        objectMapper.registerModule(simpleModule);
        // 时间格式化
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setDateFormat(DateFormat.getDateInstance());
        objectMapper.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        // 设置格式化内容
        converter.setObjectMapper(objectMapper);
        converters.add(0, converter);
    }


}