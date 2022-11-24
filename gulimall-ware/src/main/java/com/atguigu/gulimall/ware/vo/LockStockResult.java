package com.atguigu.gulimall.ware.vo;

import lombok.Data;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/24 19:23
 * 库存的锁定结果
 */
@Data
public class LockStockResult {

    private Long skuId;

    private Integer num;

    private Boolean locked;

}
