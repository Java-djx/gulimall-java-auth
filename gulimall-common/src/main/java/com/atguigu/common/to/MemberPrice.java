/**
  * Copyright 2019 bejson.com 
  */
package com.atguigu.common.to;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Auto-generated: 2019-11-26 10:50:34
 *
 * @author bejson.com (i@bejson.com)
 * @website http://www.bejson.com/java2pojo/
 */
@Data
public class MemberPrice {

    /**
     * id
     * sku_id
     * 会员等级id
     * 会员等级名
     * 会员对应价格
     * 可否叠加其他优惠[0-不可叠加优惠，1-可叠加]
     */

    private Long id;
    private String name;
    private BigDecimal price;

}