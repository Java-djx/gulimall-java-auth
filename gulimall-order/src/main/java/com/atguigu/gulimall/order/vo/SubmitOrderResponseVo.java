package com.atguigu.gulimall.order.vo;

import com.atguigu.gulimall.order.entity.OrderEntity;
import lombok.Data;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/24 13:55
 * 下单返回实体类
 */
@Data
public class SubmitOrderResponseVo {

    private OrderEntity order;//订单记录

    private Integer code;//返回状态码 0 成功

}
