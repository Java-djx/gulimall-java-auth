package com.atguigu.gulimall.seckill.scheduled;

import com.atguigu.gulimall.seckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;


/*
 * 商品秒杀定时任务的上架
 * @return
 * @author djx
 * @deprecated: Talk is cheap,show me the code
 * @date 2022/11/30 10:42
 */

/**
 * 秒杀商品定时上架
 * 每天晚上3点，上架最近三天需要三天秒杀的商品
 * 当天00:00:00 - 23:59:59
 * 明天00:00:00 - 23:59:59
 * 后天00:00:00 - 23:59:59
 */
@Slf4j
@Service
public class SeckillScheduled {


    @Autowired
    private SeckillService seckillServicel;


    @Autowired
    private RedissonClient redissonClient;

    private final String upload_lock = "seckill:upload:lock";

    //TODO 保证幂等性问题
    // @Scheduled(cron = "*/5 * * * * ? ")
    @Scheduled(cron = "0 0 1/1 * * ? ")
    public void uploadSeckillSkuLatest3Days() {
        //1、重复上架无需处理
        log.info("上架秒杀的商品...");
        //添加分布式锁
        RLock lock = redissonClient.getLock(upload_lock);
        lock.lock(10,TimeUnit.SECONDS);
        try {
            seckillServicel.uploadSeckillSkuLatest3Days();
        }catch (Exception e){
            lock.unlock();
        }
    }
}
