package com.atguigu.gulimall.cart.vo;

import lombok.Data;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/19 20:28
 */
@Data
public class UserInfoTo {

    private Long userId;

    private String userKey;

    private Boolean tempUser=false;

}
