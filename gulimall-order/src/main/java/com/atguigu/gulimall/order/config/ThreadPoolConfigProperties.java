package com.atguigu.gulimall.order.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/16 20:25
 */
@ConfigurationProperties(prefix = "gulimall.thread")
@Component
@Data
public class ThreadPoolConfigProperties {

    private int corePoolSize;
    private int maximumPoolSize;
    private long keepAliveTime;

}
