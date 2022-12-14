package com.atguigu.gulimall.product;


import com.atguigu.gulimall.product.entity.BrandEntity;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.BrandService;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.service.SkuInfoService;
import com.atguigu.gulimall.product.vo.SkuItemVo;
import com.atguigu.gulimall.product.vo.SpuItemAttrGroupVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;


@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class GulimallProductApplicationTests {

    @Autowired
    private BrandService brandService;


    @Autowired
    private CategoryService categoryService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private SkuInfoService skuInfoService;


    @Test
    public void skuInfoInfo() throws ExecutionException, InterruptedException {
        SkuItemVo item = skuInfoService.item(3L);

        List<SpuItemAttrGroupVo> attrGroupAttrs = item.getAttrGroupAttrs();
        attrGroupAttrs.forEach(spuItemAttrGroupVo ->
        {
            System.out.println(spuItemAttrGroupVo);
        });
    }



    @Test
    public void testRedissonClient() {
        System.out.println("redissonClient = " + redissonClient);
    }


    @Test
    public void testCatelogPath() {
        Long[] catelogPath = categoryService.findCatelogPath(225L);
        log.info("????????????{}", Arrays.asList(catelogPath));
    }


    @Test
    public void contextLoads() {

//        BrandEntity brandEntity=new BrandEntity();
//        brandEntity.setDescript("??????????????????C");
//        brandEntity.setName("??????");
//        brandEntity.setBrandId(1L);
//        boolean save = brandService.save(brandEntity);
//
//        if (save) {
//            System.out.println("????????????");
//        }
//       brandService.updateById(brandEntity);

        QueryWrapper<BrandEntity> queryWrapper = new QueryWrapper<BrandEntity>();

        queryWrapper.eq("name", "??????");


        List<BrandEntity> list = brandService.list(queryWrapper);
        list.forEach((item) -> {
            System.out.println(item);
        });

    }

    /**
     * ??????Redis
     */
    @Test
    public void testRedisCache() {

        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();

        //???????????????
        opsForValue.set("hello", "world"+ UUID.randomUUID().toString());

        String hello = opsForValue.get("hello");

        System.out.println("hello = " + hello);

    }

}
