package com.atguigu.gulimall.coupon.service;

import com.atguigu.common.to.SpuBoundTo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.coupon.entity.SpuBoundsEntity;

import java.util.Map;

/**
 * 商品spu积分设置
 *
 * @author djx
 * @email djx@gmail.com
 * @date 2022-11-02 15:46:06
 */
public interface SpuBoundsService extends IService<SpuBoundsEntity> {

    PageUtils queryPage(Map<String, Object> params);

}

