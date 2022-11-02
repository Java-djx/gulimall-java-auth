package com.atguigu.gulimall.coupon.dao;

import com.atguigu.gulimall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author djx
 * @email djx@gmail.com
 * @date 2022-11-02 15:46:06
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
