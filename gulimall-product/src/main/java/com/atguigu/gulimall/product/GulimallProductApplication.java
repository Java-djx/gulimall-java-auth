package com.atguigu.gulimall.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 整合SpringCache:
 *      原理:
 *      1、CacheAutoConfiguration -> 自动注入了 RedisCacheConfiguration
 *      2、自动配置了RedisCacheConfiguration - 初始化所有缓存消息 - 每个缓存决定用什么配置
 *      3、如果 RedisCacheConfiguration 有就有已有的 没有就用默认配置
 *      4、想要修改缓存配置，只需要给容器中放一个 RedisCacheConfiguration 即可
 *      5、就会应用到当前 RedisCacheManager 管理的所有缓存分区中
 */


@SpringBootApplication
@MapperScan("com.atguigu.gulimall.product.dao")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.atguigu.gulimall.product.feign")
@EnableRedisHttpSession
public class GulimallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallProductApplication.class, args);
    }

}
