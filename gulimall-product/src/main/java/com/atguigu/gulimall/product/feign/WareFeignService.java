package com.atguigu.gulimall.product.feign;

import com.atguigu.common.utils.R;
import com.atguigu.gulimall.product.vo.SkuHasStockVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/9 15:59
 */
@FeignClient("gulimall-ware")
public interface WareFeignService {

    /**
     * 解决返回值类型
     * 1、在返回类型上加上泛型
     * 2、直接返回需要的结果
     * 3、自己封装解析结果
     *
     * @param skuIds
     * @return
     * 查询sku是否有存库
     */
    @RequestMapping("/ware/waresku/hasStock")
    R hasStock(@RequestParam List<Long> skuIds);

}
