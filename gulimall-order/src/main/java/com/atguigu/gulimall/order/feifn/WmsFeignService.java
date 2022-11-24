package com.atguigu.gulimall.order.feifn;

import com.atguigu.common.utils.R;
import com.atguigu.gulimall.order.vo.WareSkuLockVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/22 20:23
 */
@FeignClient("gulimall-ware")
public interface WmsFeignService {

    /*
     * 查询是否有货
     * @return
     * @author djx
     * @deprecated: Talk is cheap,show me the code
     * @date 2022/11/24 14:40
     */
    @RequestMapping("ware/waresku/hasStock")
    public R hasStock(@RequestParam List<Long> skuIds);


    /*
     * 计算运费
     * @return
     * @author djx
     * @deprecated: Talk is cheap,show me the code
     * @date 2022/11/24 14:40
     */
    @GetMapping("/ware/wareinfo/fare")
    public R getFare(@RequestParam("addrId") Long addrId);


    /*
     * 锁定库存
     * @return
     * @author djx
     * @deprecated: Talk is cheap,show me the code
     * @date 2022/11/24 19:22
     */
    @PostMapping("/ware/waresku/lock/order")
    public R orderLockStock(@RequestBody WareSkuLockVo vo);


}
