package com.atguigu.gulimall.ware.service;

import com.atguigu.common.to.mq.OrderTo;
import com.atguigu.common.to.mq.StockLockedTo;
import com.atguigu.gulimall.ware.vo.LockStockResult;
import com.atguigu.gulimall.ware.vo.SkuHasStockVo;
import com.atguigu.gulimall.ware.vo.WareSkuLockVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.ware.entity.WareSkuEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author djx
 * @email djx@gmail.com
 * @date 2022-11-02 16:07:55
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addStock(Integer skuNum, Long skuId, Long wareId);

    /**
     * 检查库存
     *
     * @param skuIds
     * @return
     */
    List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds);

    /*
     * 锁定库存
     * @return
     * @author djx
     * @deprecated: Talk is cheap,show me the code
     * @date 2022/11/24 19:28
     */
    Boolean orderLockStock(WareSkuLockVo vo);

     /*
      * 解锁库存
      * @return
      * @author djx
      * @deprecated: Talk is cheap,show me the code
      * @date 2022/11/28 20:00
      */
    void unlockStock(StockLockedTo to);

     /*
      * 解锁订单
      *  防止订单服务卡顿
      * @return
      * @author djx
      * @deprecated: Talk is cheap,show me the code
      * @date 2022/11/28 20:21
      */
    void orderLockStock(OrderTo orderTo);
}

