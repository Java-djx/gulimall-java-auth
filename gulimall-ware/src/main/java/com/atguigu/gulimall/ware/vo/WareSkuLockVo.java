package com.atguigu.gulimall.ware.vo;

import lombok.Data;

import java.util.List;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/24 19:19
 * 库存锁定库存
 */
@Data
public class WareSkuLockVo {



    private String orderSn;//订单号

    private List<OrderItemVo> locks;//需要锁定的库存消息


}
