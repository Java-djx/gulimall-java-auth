package com.atguigu.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberResponseVo;
import com.atguigu.gulimall.order.constant.OrderConstant;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.atguigu.gulimall.order.feifn.CartFeignService;
import com.atguigu.gulimall.order.feifn.MemberFeignService;
import com.atguigu.gulimall.order.feifn.WmsFeignService;
import com.atguigu.gulimall.order.interceptor.OrderInterceptor;
import com.atguigu.gulimall.order.to.OrderCreateTo;
import com.atguigu.gulimall.order.vo.*;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.order.dao.OrderDao;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.service.OrderService;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    private ThreadLocal<OrderSubmitVo> confirmVoThreadLocal = new ThreadLocal<OrderSubmitVo>();

    //远程查询会员
    @Autowired
    private MemberFeignService memberFeignService;

    @Autowired
    private ThreadPoolExecutor executor;

    //远程调用查询购物车
    @Autowired
    private CartFeignService cartFeignService;

    @Autowired
    private WmsFeignService wmsFeignService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        //TODO :获取当前线程请求头信息(解决Feign异步调用丢失请求头问题)
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        MemberResponseVo memberResponseVo = OrderInterceptor.threadLocal.get();
        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            //1、远程查询会员地址
            List<MemberAddressVo> address = memberFeignService.getAddress(memberResponseVo.getId());
            //每一个线程都来共享之前的请求数据
            RequestContextHolder.setRequestAttributes(requestAttributes);
            confirmVo.setAddress(address);
        }, executor);
        CompletableFuture<Void> getCartFuture = CompletableFuture.runAsync(() -> {
            //2、远程查询购物车所有选中的购物车项
//            List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
            //每一个线程都来共享之前的请求数据
            RequestContextHolder.setRequestAttributes(requestAttributes);
            R r = cartFeignService.getCurrentUserCartItems();
            List<OrderItemVo> data = r.getData(new TypeReference<List<OrderItemVo>>() {
            });
            confirmVo.setItems(data);
        }, executor).thenRunAsync(() -> {
            List<OrderItemVo> items = confirmVo.getItems();
            List<Long> ids = items.stream().map(item -> {
                return item.getSkuId();
            }).collect(Collectors.toList());
            R r = wmsFeignService.hasStock(ids);
            List<SkuStockVo> data = r.getData(new TypeReference<List<SkuStockVo>>() {
            });
            if (data != null && data.size() > 0) {
                //将skuStockVos集合转换为map
                Map<Long, Boolean> skuHasStockMap = data.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                confirmVo.setStocks(skuHasStockMap);
            }
        }, executor);
        //3.查询用户的积分
        Integer integration = memberResponseVo.getIntegration();
        confirmVo.setIntegration(integration);
        CompletableFuture.allOf(getAddressFuture, getCartFuture).get();
        //4.其他数据自动计算

        //TODO 订单防重令牌
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId(), token, 30, TimeUnit.MINUTES);
        confirmVo.setOrderToken(token);


        return confirmVo;
    }

    /*
     *  流程：
     *     1.验证令牌
     *     2.
     * @return
     * @author djx
     * @deprecated: Talk is cheap,show me the code
     * @date 2022/11/24 13:59
     */
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {
        SubmitOrderResponseVo response = new SubmitOrderResponseVo();
        MemberResponseVo memberResponseVo = OrderInterceptor.threadLocal.get();
        confirmVoThreadLocal.set(vo);
        //1.验证令牌 从redis中取出令牌[令牌的对比和删除必须保证原子性]
        // 脚本结果 0 令牌校验失败 - 1 删除成功
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        String orderToken = vo.getOrderToken();
        //原子校验
        Long result = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId()), orderToken);

        if (result == 0L) {
            //token验证失败
            response.setCode(1);
            return response;
        } else {
            //token验证通过
            response.setCode(0);
            //1.创建订单
            OrderCreateTo createTo = creatOrder();
        }

        /**
         *  为保证原子性使用lun脚本
         *         //从redis取出token
         *         String redisToken = redisTemplate.opsForValue().get(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId());
         *         if (orderToken!=null && orderToken.equals(redisToken)){
         *             //token验证通过
         *             //删除令牌
         *             redisTemplate.delete(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId());
         *         }else {
         *             //验证不通过
         *         }
         */

        return response;
    }

    /*
     * 创建订单
     * @return
     * @author djx
     * @deprecated: Talk is cheap,show me the code
     * @date 2022/11/24 14:33
     */
    private OrderCreateTo creatOrder() {
        //1.创建订单
        OrderCreateTo createTo = new OrderCreateTo();
        //1.生成订单流水号
        String orderSn = IdWorker.getTimeId();
        OrderEntity orderEntity = buildOrder(orderSn);
        //2.获取所有订单项数据
        List<OrderItemEntity> itemsEntity = buildOrderItems(orderSn);

        //3.比较价格
        return createTo;
    }

    /*
     * 构建订单
     * @return
     * @author djx
     * @deprecated: Talk is cheap,show me the code
     * @date 2022/11/24 15:31
     */
    private OrderEntity buildOrder(String orderSn) {
        //创建订单实体类
        OrderEntity entity = new OrderEntity();
        entity.setOrderSn(orderSn);
//        entity.setStatus();
        //1.获取收货地址
        OrderSubmitVo submitVo = confirmVoThreadLocal.get();
        //远程获取到收货地址消息
        R fare = wmsFeignService.getFare(submitVo.getAddrId());
        FareVo fareResp = fare.getData(new TypeReference<FareVo>() {
        });
        //设置运费金额
        entity.setFreightAmount(fareResp.getFare());
        //设置收货人消息   地址消息
        entity.setReceiverCity(fareResp.getAddress().getCity());
        //收货人详情
        entity.setReceiverDetailAddress(fareResp.getAddress().getDetailAddress());
        //设置收货人名字
        entity.setReceiverName(fareResp.getAddress().getName());
        //手机号
        entity.setReceiverPhone(fareResp.getAddress().getPhone());
        //收货人邮编
        entity.setReceiverPostCode(fareResp.getAddress().getPostCode());
        //省份/直辖市
        entity.setReceiverProvince(fareResp.getAddress().getProvince());
        // 区
        entity.setReceiverRegion(fareResp.getAddress().getRegion());

        return entity;
    }

    /*
     * 构建所有订单项数据
     * @return
     * @author djx
     * @deprecated: Talk is cheap,show me the code
     * @date 2022/11/24 15:24
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        //2.从购物车中取出商品项
        R r = cartFeignService.getCurrentUserCartItems();
        List<OrderItemVo> data = r.getData(new TypeReference<List<OrderItemVo>>() {
        });
        if (data != null && data.size() > 0) {
            //处理购物车中的商品项
            List<OrderItemEntity> orderItemEntities = data.stream().map(orderItemVo -> {
                OrderItemEntity orderItemEntity = buildOrderItem(orderItemVo);
                orderItemEntity.setOrderSn(orderSn);
                return orderItemEntity;
            }).collect(Collectors.toList());
        }
        return null;
    }

    /*
     * 构建每一个订单项数据
     * @return
     * @author djx
     * @deprecated: Talk is cheap,show me the code
     * @date 2022/11/24 15:29
     */
    private OrderItemEntity buildOrderItem(OrderItemVo cartItem) {
        OrderItemEntity orderItemEntity = new OrderItemEntity();

        //1.订单消息 订单号

        //2.商品的spu
        Long skuId = cartItem.getSkuId();


        //3.商品的sku
        orderItemEntity.setSkuId(cartItem.getSkuId());
        List<String> attr = cartItem.getSkuAttr();
        String skuAttrValues = StringUtils.collectionToDelimitedString(attr, ";");
        orderItemEntity.setSkuAttrsVals(skuAttrValues);
        orderItemEntity.setSkuName(cartItem.getTitle());
        orderItemEntity.setSkuPic(cartItem.getImages());
        orderItemEntity.setSkuPrice(cartItem.getPrice());
        orderItemEntity.setSkuQuantity(cartItem.getCount());
        //4.优惠

        //5.积分消息
        orderItemEntity.setGiftGrowth(cartItem.getPrice().intValue());
        orderItemEntity.setGiftIntegration(cartItem.getPrice().intValue());


        return orderItemEntity;
    }

}