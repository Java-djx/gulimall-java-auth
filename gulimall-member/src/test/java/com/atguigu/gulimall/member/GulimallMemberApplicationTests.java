package com.atguigu.gulimall.member;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.Md5Crypt;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.junit4.SpringRunner;

//@RunWith(SpringRunner.class)
//@SpringBootTest
public class GulimallMemberApplicationTests {

    @Test
    public void contextLoads() {
        String md5Hex = DigestUtils.md5Hex("1234566");
        System.out.println(md5Hex);

        String md5Crypt = Md5Crypt.md5Crypt("123345".getBytes());


        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

        String encode = bCryptPasswordEncoder.encode("admin123");

        System.out.println(bCryptPasswordEncoder.matches("admin123", "$2a$10$VDdUECOnuzW15U1HdxLF/Ok2UQwg/20TwmPSXmk4/APIRGWyZXH1S"));

        System.out.println(encode);
    }

}
