package com.atguigu.gulimall.thirdparty;

import com.aliyun.oss.OSSClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GulimallThirdPartyApplicationTests {

    @Test
    public void contextLoads() {
    }

    @Resource
    private   OSSClient ossClient;

    @Test
    public void contextLoads2() throws FileNotFoundException {
        InputStream inputStream = new
                FileInputStream("D:\\myshuaizhao.png");
        ossClient.putObject("gulimall-djx", "gulimall/hahhaha.png", inputStream);
        System.out.println("ok");
    }

}
