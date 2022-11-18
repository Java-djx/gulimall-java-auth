package com.atguigu.gulimall.member.vo;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/17 18:48
 */
@Data
public class MemberRegisterVo {

    private String userName;

    private String password;

    private String phone;


}