package com.atguigu.gulimall.product.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/11 19:51
 */
@Configuration
public class MyRedissonConfig {

    /**
     * 所有对Redisson 操作都需要RedissonClient
     * @return
     * @throws IOException
     */
    @Bean(destroyMethod="shutdown")
    public RedissonClient redisson() throws IOException {
        //1、创建配置
        Config config = new Config();
        //2、创建配置
        config.useSingleServer().setAddress("redis://192.168.184.129:6379");
        return Redisson.create(config);
    }

}
