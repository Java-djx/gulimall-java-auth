package com.atguigu.common.to.mq;

import lombok.Data;

import java.util.List;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/28 15:58
 * 库存锁定to
 */
@Data
public class StockLockedTo {


    private Long id;//库存工作单

    private StockDetailTo detail;//工作单详情的id

}
