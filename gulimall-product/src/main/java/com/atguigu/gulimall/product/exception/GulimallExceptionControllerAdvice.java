package com.atguigu.gulimall.product.exception;

import com.atguigu.common.exception.BizCodeEnume;
import com.atguigu.common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/4 12:59
 * 自定义异常类 集中处理异常
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.atguigu.gulimall.product.controller")
public class GulimallExceptionControllerAdvice {


    /**
     * 处理数据不合法自定义异常
     *
     * @param e
     * @return
     */
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public R handlerVaildException(MethodArgumentNotValidException e) {
        log.error("数据校验出现异常:{},异常类型:{}", e.getMessage(), e.getClass());
        BindingResult result = e.getBindingResult();
        Map<String, String> map = new HashMap<String, String>();
        //获取校验的错误结果
        result.getFieldErrors().forEach(item -> {
            //获取错误消息的提示
            String message = item.getDefaultMessage();
            //获取错误的属性的名字
            String field = item.getField();
            map.put("message", message);
            map.put("field", field);
        });
        return R.error(BizCodeEnume.VAILD_EXCEPTION.getCode(), BizCodeEnume.VAILD_EXCEPTION.getMsg()).put("data", map);
    }

    /**
     * 全局异常
     *
     * @param throwable
     * @return
     */
    @ExceptionHandler(value = Throwable.class)
    public R handlerException(Throwable throwable) {
        log.error("全局异:{},异常类型:{}", throwable.getMessage(), throwable.getClass());
        return R.error(BizCodeEnume.UNKNOW_EXCEPTION.getCode(), BizCodeEnume.UNKNOW_EXCEPTION.getMsg());
    }

}
