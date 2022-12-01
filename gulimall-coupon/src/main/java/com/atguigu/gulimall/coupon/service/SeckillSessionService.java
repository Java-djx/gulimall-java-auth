package com.atguigu.gulimall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.coupon.entity.SeckillSessionEntity;

import java.util.List;
import java.util.Map;

/**
 * 秒杀活动场次
 *
 * @author djx
 * @email djx@gmail.com
 * @date 2022-11-02 15:46:06
 */
public interface SeckillSessionService extends IService<SeckillSessionEntity> {

    PageUtils queryPage(Map<String, Object> params);

     /*
      * 最近三天上架的秒杀活动
      * @return
      * @author djx
      * @deprecated: Talk is cheap,show me the code
      * @date 2022/11/30 10:59
      */
    List<SeckillSessionEntity> getlatest3Days();

}

