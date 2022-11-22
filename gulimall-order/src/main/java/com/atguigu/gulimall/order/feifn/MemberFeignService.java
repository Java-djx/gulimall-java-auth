package com.atguigu.gulimall.order.feifn;

import com.atguigu.gulimall.order.vo.MemberAddressVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/21 20:52
 * 远程调用会员服务
 */
@FeignClient("gulimall-member")
public interface MemberFeignService {

    @GetMapping("/member/memberreceiveaddress/{memberId}/addrsses")
    public List<MemberAddressVo> getAddress(@PathVariable("memberId") Long memberId);

}
