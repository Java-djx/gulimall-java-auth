package com.atguigu.gulimall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.seckill.feign.CouponFeignService;
import com.atguigu.gulimall.seckill.feign.ProductFeignService;
import com.atguigu.gulimall.seckill.service.SeckillService;
import com.atguigu.gulimall.seckill.to.SeckillSkuRedisTo;
import com.atguigu.gulimall.seckill.vo.SeckillSessionWithSkusVo;
import com.atguigu.gulimall.seckill.vo.SeckillSkuVo;
import com.atguigu.gulimall.seckill.vo.SkuInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/30 10:56
 */
@Service
@Slf4j
public class SeckillServiceImpl implements SeckillService {


    @Autowired
    private CouponFeignService couponFeignService;


    @Autowired
    private ProductFeignService productFeignService;


    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private StringRedisTemplate redisTemplate;


    private final String SESSION__CACHE_PREFIX = "seckill:sessions:";

    private final String SECKILL_CHARE_PREFIX = "seckill:skus";

    private final String SKU_STOCK_SEMAPHORE = "seckill:stock:";//+商品随机码·


    /*
     * 上架最近三天秒杀的商品
     * @return
     * @author djx
     * @deprecated: Talk is cheap,show me the code
     * @date 2022/11/30 10:56
     */
    @Override
    public void uploadSeckillSkuLatest3Days() {
        R r = couponFeignService.getlatest3Days();
        if (r.getCode() == 0) {
            //获取到最近三天的消息
            List<SeckillSessionWithSkusVo> data = r.getData(new TypeReference<List<SeckillSessionWithSkusVo>>() {
            });
            //缓存到redis
            //1.缓存活动消息
            saveSessionInfos(data);
            //2.缓存活动对应的商品消息
            saveSessionSkuInfos(data);
        }
    }

    /*
     * 上架 .缓存活动消息
     * @return
     * @author djx
     * @deprecated: Talk is cheap,show me the code
     * @date 2022/11/30 11:39
     */
    public void saveSessionInfos(List<SeckillSessionWithSkusVo> withSkusVo) {
        withSkusVo.stream().forEach(item -> {
            Long startTime = item.getStartTime().getTime();
            Long endTime = item.getEndTime().getTime();
            //存入到Redis中的key
            String key = SESSION__CACHE_PREFIX + startTime + "_" + endTime;
            Boolean hasKey = redisTemplate.hasKey(key);
            //缓存活动下辖
            if (!hasKey) {
                //获取到活动中
                List<String> collect = item.getRelationSkus().stream()
                        .map(p -> p.getPromotionId()+"_"+p.getSkuId()).collect(Collectors.toList());
                redisTemplate.opsForList().leftPushAll(key, collect);
            }
        });
    }

    /*
     * 缓存活动对应的商品消息
     * @return
     * @author djx
     * @deprecated: Talk is cheap,show me the code
     * @date 2022/11/30 11:39
     */
    public void saveSessionSkuInfos(List<SeckillSessionWithSkusVo> skusVo) {
        //上架可以秒杀的商品
        skusVo.stream().forEach(session -> {
            //准备hash操作
            BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(SECKILL_CHARE_PREFIX);
            session.getRelationSkus().stream().forEach(seckilldSkuVo -> {
                //生成随机码
                String token = UUID.randomUUID().toString().replace("-", "");
                if (!ops.hasKey(seckilldSkuVo.getPromotionId().toString()+"_"+seckilldSkuVo.getSkuId().toString())) {
                    //缓存商品
                    SeckillSkuRedisTo redisTo = new SeckillSkuRedisTo();
                    //1.sku的基本消息
                    R r = productFeignService.getSkuInfo(seckilldSkuVo.getSkuId());
                    if (r.getCode() == 0) {
                        SkuInfoVo skuInfo = r.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                        });
                        redisTo.setSkuInfo(skuInfo);
                    }
                    //2.sku的秒杀消息
                    BeanUtils.copyProperties(seckilldSkuVo, redisTo);
                    //3、设置当前商品的秒杀时间信息
                    redisTo.setStartTime(session.getStartTime().getTime());
                    redisTo.setEndTime(session.getEndTime().getTime());
                    //4、设置商品的随机码（防止恶意攻击）
                    redisTo.setRandomCode(token);
                    //序列化json格式存入Redis中
                    String toJSONString = JSON.toJSONString(redisTo);
                    ops.put(seckilldSkuVo.getPromotionId().toString()+"_"+seckilldSkuVo.getSkuId().toString(), toJSONString);
                    //5、使用库存作为分布式Redisson信号量（限流）
                    // 使用库存作为分布式信号量
                    //如果当前这个场次的商品库存信息已经上架就不需要上架


                    RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + token);
                    semaphore.trySetPermits(seckilldSkuVo.getSeckillCount());
                }
            });
        });
    }

}
