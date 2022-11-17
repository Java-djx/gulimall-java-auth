package com.atguigu.gulimall.thirdparty.component;

import com.atguigu.gulimall.thirdparty.utiles.HttpUtils;
import lombok.Data;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/17 15:38
 */
@Component
@Data
@ConfigurationProperties("spring.cloud.alicloud.sms")
public class SmsComponent {

    private String host;
    private String path;
    private String appcode;

    public void sendSmsCode(String phone, String code) throws Exception {
        String method = "POST";
        Map<String, String> headers = new HashMap<String, String>();
        //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
        headers.put("Authorization", "APPCODE " + appcode);
        //根据API的要求，定义相对应的Content-Type
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        Map<String, String> querys = new HashMap<String, String>();
        Map<String, String> bodys = new HashMap<String, String>();
        bodys.put("content", "code:" + code);
        bodys.put("phone_number", phone);
        bodys.put("template_id", "TPL_0000");
        HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
        //获取response的body
        System.out.println(EntityUtils.toString(response.getEntity()));
    }

}
