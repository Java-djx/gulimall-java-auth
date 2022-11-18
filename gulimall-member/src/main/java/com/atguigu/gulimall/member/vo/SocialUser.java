package com.atguigu.gulimall.member.vo;

import lombok.Data;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/18 22:31
 */
@Data
public class SocialUser {

    private String access_token;

    private String remind_in;

    private long expires_in;

    private String uid;

    private String isRealName;
}
