package com.atguigu.gulimall.seckill.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/30 11:14
 */
@FeignClient("gulimall-coupon")
public interface CouponFeignService {

     /*
      * 查询最近三天需要秒杀的商品
      * @return
      * @author djx
      * @deprecated: Talk is cheap,show me the code
      * @date 2022/11/30 11:15
      */
    @GetMapping("/coupon/seckillsession/getlatest3Days")
    R getlatest3Days();
}
