package com.atguigu.gulimall.member.exception;

/**
 * @author djx
 * @email djx@gmail.com
 * @date 2022-11-02 15:50:28
 **/
public class PhoneException extends RuntimeException {

    public PhoneException() {
        super("存在相同的手机号");
    }
}
