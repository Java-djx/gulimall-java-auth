package com.atguigu.gulimall.cart.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/19 22:03
 */
@FeignClient("gulimall-product")
public interface ProductFeignService {

    /*
     * 根据skuId查询商品详情
     * @return
     * @author djx
     * @deprecated: Talk is cheap,show me the code
     * @date 2022/11/19 22:04
     */
    @RequestMapping("/product/skuinfo/info/{skuId}")
    public R getSkuInfo(@PathVariable("skuId") Long skuId);

    /*
     * 根据skuId查询sku的销售属性
     * @return
     * @author djx
     * @deprecated: Talk is cheap,show me the code
     * @date 2022/11/19 22:21
     */
    @GetMapping("/product/skusaleattrvalue/stringsList/{skuId}")
    public List<String> getSkuSaleAttrValues(@PathVariable("skuId") Long skuId);
}
