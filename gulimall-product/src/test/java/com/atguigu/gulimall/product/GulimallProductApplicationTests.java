package com.atguigu.gulimall.product;



import com.atguigu.gulimall.product.entity.BrandEntity;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.BrandService;
import com.atguigu.gulimall.product.service.CategoryService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;


@RunWith(SpringRunner.class)
@SpringBootTest
public class GulimallProductApplicationTests {

    @Autowired
    private BrandService brandService;

   @Test
   public void contextLoads() {

//        BrandEntity brandEntity=new BrandEntity();
//        brandEntity.setDescript("华为手机把把C");
//        brandEntity.setName("华为");
//        brandEntity.setBrandId(1L);
//        boolean save = brandService.save(brandEntity);
//
//        if (save) {
//            System.out.println("保存成功");
//        }
//       brandService.updateById(brandEntity);

       QueryWrapper<BrandEntity> queryWrapper = new QueryWrapper<BrandEntity>();

       queryWrapper.eq("name","华为");


       List<BrandEntity> list = brandService.list(queryWrapper);
       list.forEach((item)->{
           System.out.println(item);
       });

   }

}
