package com.atguigu.common.to.mq;


import lombok.Data;

import java.io.Serializable;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/28 16:03
 * 记录工作单详情
 */
@Data
public class StockDetailTo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;
    /**
     * sku_id
     */
    private Long skuId;
    /**
     * sku_name
     */
    private String skuName;
    /**
     * 购买个数
     */
    private Integer skuNum;
    /**
     * 工作单id
     */
    private Long taskId;

    private Long wareId;

    private Integer lockStatus;
}
