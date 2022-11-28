package com.atguigu.gulimall.order.service;

import com.atguigu.gulimall.order.vo.OrderConfirmVo;
import com.atguigu.gulimall.order.vo.OrderSubmitVo;
import com.atguigu.gulimall.order.vo.SubmitOrderResponseVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.order.entity.OrderEntity;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 订单
 *
 * @author djx
 * @email djx@gmail.com
 * @date 2022-11-02 16:12:15
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

     /*
      * 跳转到订单页面查看订单的数据
      * @return 
      * @author djx
      * @deprecated: Talk is cheap,show me the code
      * @date 2022/11/24 13:59
      */
    OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException;

     /*
      * 下单 
      * @return 
      * @author djx
      * @deprecated: Talk is cheap,show me the code
      * @date 2022/11/24 13:58
      */
    SubmitOrderResponseVo submitOrder(OrderSubmitVo vo);

     /*
      * 根据订单编号查询订单状态
      * @return
      * @author djx
      * @deprecated: Talk is cheap,show me the code
      * @date 2022/11/28 18:57
      */
    OrderEntity getOrderStatusBySn(String orderSn);

     /*
      * 订单超时未支付关闭订单
      * @return
      * @author djx
      * @deprecated: Talk is cheap,show me the code
      * @date 2022/11/28 18:57
      */
    void closeOrder(OrderEntity entity);
}

