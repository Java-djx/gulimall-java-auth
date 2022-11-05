package com.atguigu.gulimall.member;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 远程调用的步骤
 *  1.引入openFeign
 *  2.编写一个接口，告诉cloud 接口需要调用远程调用
 *   1、声明接口的每一个方法都是哪个微服务的哪个资源
 *  3. 开启OpenFeign 功能
 */
@SpringBootApplication
@MapperScan("com.atguigu.gulimall.member.dao")
@EnableDiscoveryClient
@EnableFeignClients("com.atguigu.gulimall.member.fegin")
public class GulimallMemberApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallMemberApplication.class, args);
    }

}
