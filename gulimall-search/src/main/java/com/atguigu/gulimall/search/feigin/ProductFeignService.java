package com.atguigu.gulimall.search.feigin;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/14 18:37
 */
@FeignClient("gulimall-product")
public interface ProductFeignService {


    @GetMapping("/product/attr/info/{attrId}")
    public R attrInfo(@PathVariable("attrId") Long attrId);

    @GetMapping("/product/brand/infos")
    public R BrandsInfos(@RequestParam("brandIds") List<Long> brandIds);
}
