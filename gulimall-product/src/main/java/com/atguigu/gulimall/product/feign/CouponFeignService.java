package com.atguigu.gulimall.product.feign;

import com.atguigu.common.to.SkuReductionTo;
import com.atguigu.common.to.SpuBoundTo;
import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/5 18:31
 */
@FeignClient("gulimall-coupon")
public interface CouponFeignService {


    /**
     * 远程调用优惠券服务添加积分和成长值
     * CouponFeignService.saveSpuBounds(spuBoundTo);
     * 1)、@RequestBody 将对象装换为json
     * 2)。找到gulimall-coupond服务的/coupon/spubounds/save发送请求
     * 将上一步转的json放在请求体中,发送数据
     * 3）,对方服务收到请求,请求体中有json数据
     * 只要json数据模型是兼容的,对方服务无需更换 TO
     *
     * @param spuBoundTo
     * @return
     */
    @PostMapping("/coupon/spubounds/save")
    R saveSpuBounds(@RequestBody SpuBoundTo spuBoundTo);

    /**
     * 远程调用优惠券库存 实现
     * // ->sms_sku_ladder 优惠
     * // ->sms_sku_full_reduction 满减
     * // ->sms_member_price 会员打折
     *
     * @param skuReductionTo
     * @return
     */
    @PostMapping("/coupon/skufullreduction/saveInfo")
    public R saveInfo(@RequestBody SkuReductionTo skuReductionTo);
}
