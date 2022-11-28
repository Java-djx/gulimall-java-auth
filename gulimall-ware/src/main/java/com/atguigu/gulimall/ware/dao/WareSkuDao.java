package com.atguigu.gulimall.ware.dao;

import com.atguigu.gulimall.ware.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品库存
 * 
 * @author djx
 * @email djx@gmail.com
 * @date 2022-11-02 16:07:55
 */
@Mapper
public interface WareSkuDao extends BaseMapper<WareSkuEntity> {

   List<Long> listWareIdHasSkuStock(@Param("skuId") Long skuId);

    void addStock(@Param("skuNum") Integer skuNum, @Param("skuId") Long skuId, @Param("wareId") Long wareId);


    /**
     * 查询每个商品在库存中的库存数量
     * @param skuId
     * @return
     */
    Long getSkuStock(@Param("skuId") Long skuId);

    Long lockSkuStock(@Param("skuId") Long skuId, @Param("wareId") Long wareId, @Param("num") Integer num);

    void unLockStock(@Param("skuId") Long skuId, @Param("wareId") Long wareId, @Param("num") Integer num);
}
