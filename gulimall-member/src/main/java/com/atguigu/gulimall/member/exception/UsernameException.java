package com.atguigu.gulimall.member.exception;

/**
 * @author djx
 * @email djx@gmail.com
 * @date 2022-11-02 15:50:28
 **/
public class UsernameException extends RuntimeException {


    public UsernameException() {
        super("存在相同的用户名");
    }
}
