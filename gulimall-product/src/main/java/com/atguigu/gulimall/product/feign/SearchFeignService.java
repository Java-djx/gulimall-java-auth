package com.atguigu.gulimall.product.feign;

import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/9 17:53
 */
@FeignClient("gulimall-search")
public interface SearchFeignService {


    @PostMapping("/search/save/product")
    public R productStatusUp(@RequestBody List<SkuEsModel> esModels);

}
