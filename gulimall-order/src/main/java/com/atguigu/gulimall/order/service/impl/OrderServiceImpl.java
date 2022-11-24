package com.atguigu.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberResponseVo;
import com.atguigu.gulimall.order.constant.OrderConstant;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.atguigu.gulimall.order.enume.OrderStatusEnum;
import com.atguigu.gulimall.order.feifn.CartFeignService;
import com.atguigu.gulimall.order.feifn.MemberFeignService;
import com.atguigu.gulimall.order.feifn.ProductFeignService;
import com.atguigu.gulimall.order.feifn.WmsFeignService;
import com.atguigu.gulimall.order.interceptor.OrderInterceptor;
import com.atguigu.gulimall.order.service.OrderItemService;
import com.atguigu.gulimall.order.to.OrderCreateTo;
import com.atguigu.gulimall.order.vo.*;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
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
import org.springframework.transaction.annotation.Transactional;
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


    @Autowired
    private ProductFeignService productFeignService;


    @Autowired
    private OrderItemService orderItemService;


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
    @Transactional
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
        response.setCode(0);
        if (result == 0L) {
            response.setCode(1);   //token验证失败
            return response;
        } else {
            //token验证通过
            //1.创建订单
            OrderCreateTo order = creatOrder();
            //2.校验价格
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = vo.getPayPrice();
            if (Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01) { //金额对比成功

                //3.保存订单
                saveOrder(order);
                //4.库存锁定只要有异常回滚订单数据
                WareSkuLockVo wareSkuLockVo = new WareSkuLockVo();
                wareSkuLockVo.setOrderSn(order.getOrder().getOrderSn());
                List<OrderItemVo> itemVoList = order.getOrderItems().stream().map(item -> {
                    OrderItemVo itemVo = new OrderItemVo();
                    itemVo.setSkuId(item.getSkuId());
                    itemVo.setCount(item.getSkuQuantity());
                    itemVo.setTitle(item.getSkuName());
                    return itemVo;
                }).collect(Collectors.toList());
                wareSkuLockVo.setLocks(itemVoList);
                //锁定库存
                R r = wmsFeignService.orderLockStock(wareSkuLockVo);
                //TODO 远程锁定库存
                if (r.getCode() == 0) {
                    //锁定库存成功
                    response.setOrder(order.getOrder());
                    return response;
                } else {
                    //锁定失败
                    response.setCode(3);
                    return response;
                }
            } else {
                response.setCode(2);//金额对比失败
                return response;
            }
        }
    }

    /*
     * 保存订单
     * @return
     * @author djx
     * @deprecated: Talk is cheap,show me the code
     * @date 2022/11/24 19:07
     */
    private void saveOrder(OrderCreateTo order) {
        OrderEntity entity = order.getOrder();
        entity.setModifyTime(new Date());
        entity.setCreateTime(new Date());
        this.save(entity);
        List<OrderItemEntity> items = order.getOrderItems();
        orderItemService.saveBatch(items);

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
        //3.计算价格相关
        computePrice(orderEntity, itemsEntity);

        return createTo;
    }

    /*
     * 计算订单价格相关
     * @return
     * @author djx
     * @deprecated: Talk is cheap,show me the code
     * @date 2022/11/24 15:52
     */
    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> itemsEntity) {
        BigDecimal totalPrice = new BigDecimal("0");
        BigDecimal coupon = new BigDecimal("0");
        BigDecimal integrationAmount = new BigDecimal("0");
        BigDecimal promotionAmount = new BigDecimal("0");
        BigDecimal giftIntegration = new BigDecimal("0");
        BigDecimal giftGrowth = new BigDecimal("0");
        //1.订单相关的数据
        //订单的总额 叠加每一个订单项
        for (OrderItemEntity entity : itemsEntity) {
            //计算总价
            BigDecimal realAmount = entity.getRealAmount();
            totalPrice = totalPrice.add(realAmount);
            coupon = coupon.add(entity.getCouponAmount());
            integrationAmount = integrationAmount.add(entity.getIntegrationAmount());
            promotionAmount = promotionAmount.add(entity.getPromotionAmount());
            giftIntegration = giftIntegration.add(new BigDecimal(entity.getGiftIntegration().toString()));
            giftGrowth = giftGrowth.add(new BigDecimal(entity.getGiftGrowth().toString()));
        }
        //总额
        orderEntity.setTotalAmount(totalPrice);
        //应付总额
        orderEntity.setPayAmount(totalPrice.add(orderEntity.getFreightAmount()));
        orderEntity.setPromotionAmount(promotionAmount);
        orderEntity.setIntegrationAmount(integrationAmount);
        orderEntity.setCouponAmount(coupon);
        //设置积分和成长值
        orderEntity.setGrowth(giftGrowth.intValue());
        orderEntity.setIntegration(giftIntegration.intValue());
        orderEntity.setDeleteStatus(0);


    }

    /*
     * 构建订单
     * @return
     * @author djx
     * @deprecated: Talk is cheap,show me the code
     * @date 2022/11/24 15:31
     */
    private OrderEntity buildOrder(String orderSn) {

        MemberResponseVo responseVo = OrderInterceptor.threadLocal.get();
        //创建订单实体类
        OrderEntity entity = new OrderEntity();
        entity.setOrderSn(orderSn);
        entity.setMemberId(responseVo.getId());
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
        //设置相对应的订单状态
        entity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        //自动收货时间
        entity.setAutoConfirmDay(7);

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
            return orderItemEntities;
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
        R r = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo data = r.getData(new TypeReference<SpuInfoVo>() {
        });
        orderItemEntity.setSpuId(data.getId());
        orderItemEntity.setSpuName(data.getSpuName());
        orderItemEntity.setSpuBrand(data.getBrandId().toString());
        orderItemEntity.setCategoryId(data.getCatalogId());
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
        orderItemEntity.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
        orderItemEntity.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());

        //6.订单项的价格消息
        orderItemEntity.setPromotionAmount(new BigDecimal("0"));
        orderItemEntity.setCouponAmount(new BigDecimal("0"));
        orderItemEntity.setIntegrationAmount(new BigDecimal("0"));
        //当前订单项的实际
        //原来的价格 扣减全部的优惠金额
        BigDecimal orign = orderItemEntity.getSkuPrice().multiply(new BigDecimal(orderItemEntity.getSkuQuantity().toString()));
        BigDecimal subtract = orign.subtract(orderItemEntity.getCouponAmount()).subtract(orderItemEntity.getPromotionAmount()).subtract(orderItemEntity.getIntegrationAmount());
        orderItemEntity.setRealAmount(subtract);
        return orderItemEntity;
    }

}