package com.atguigu.gulimall.seckill.service;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/30 10:56
 */
public interface SeckillService {

    /**
     * 上架三天需要秒杀的商品
     */
    void uploadSeckillSkuLatest3Days();

}
