package com.atguigu.gulimall.order.feifn;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

}
