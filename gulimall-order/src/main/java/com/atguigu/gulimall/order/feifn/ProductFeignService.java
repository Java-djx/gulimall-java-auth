package com.atguigu.gulimall.order.feifn;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/24 15:46
 */
@FeignClient("gulimall-product")
public interface ProductFeignService {

    @RequestMapping("/skuId/{skuId}")
    public R getSpuInfoBySkuId(@PathVariable("skuId") Long skuId);

}
