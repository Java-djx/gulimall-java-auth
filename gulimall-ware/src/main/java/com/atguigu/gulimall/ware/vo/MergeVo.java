package com.atguigu.gulimall.ware.vo;

import lombok.Data;

import java.util.List;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/6 19:30
 */
@Data
public class MergeVo {

    /**
     * purchaseId: 1, //整单id
     * items:[1,2,3,4] //合并项集合
     */

    private Long purchaseId;

    private List<Long> items;

}
