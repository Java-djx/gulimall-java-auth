package com.atguigu.gulimall.ware.vo;

import lombok.Data;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/6 20:56
 */
@Data
public class PurchaseItemDoneVo {
    /**
     * {itemId:1,status:4,reason:""}
     */
    private Long itemId;
    private Integer status;
    private String reason;
}
