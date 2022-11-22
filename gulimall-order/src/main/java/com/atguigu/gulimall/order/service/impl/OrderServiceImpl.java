package com.atguigu.gulimall.order.service.impl;

import com.atguigu.common.vo.MemberResponseVo;
import com.atguigu.gulimall.order.feifn.CartFeignService;
import com.atguigu.gulimall.order.feifn.MemberFeignService;
import com.atguigu.gulimall.order.interceptor.OrderInterceptor;
import com.atguigu.gulimall.order.vo.MemberAddressVo;
import com.atguigu.gulimall.order.vo.OrderConfirmVo;
import com.atguigu.gulimall.order.vo.OrderItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.order.dao.OrderDao;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.service.OrderService;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    //远程查询会员
    @Autowired
    private MemberFeignService memberFeignService;

    //远程调用查询购物车
    @Autowired
    private CartFeignService cartFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() {

        OrderConfirmVo confirmVo = new OrderConfirmVo();
        MemberResponseVo memberResponseVo = OrderInterceptor.threadLocal.get();
        //1、远程查询会员地址
        List<MemberAddressVo> address = memberFeignService.getAddress(memberResponseVo.getId());
        confirmVo.setMemberAddressVos(address);
        //2、远程查询购物车所有选中的购物车项
        List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
        confirmVo.setItems(currentUserCartItems);
        //3.查询用户的积分
        Integer integration = memberResponseVo.getIntegration();
        confirmVo.setIntegration(integration);
        //4.其他数据自动计算

        //TODO 订单防重令牌


        return confirmVo;
    }

}