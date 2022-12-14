package com.atguigu.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.exception.NoStockException;
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
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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

    //??????????????????
    @Autowired
    private MemberFeignService memberFeignService;

    @Autowired
    private ThreadPoolExecutor executor;

    //???????????????????????????
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

    @Autowired
    private RabbitTemplate rabbitTemplate;


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
        //TODO :?????????????????????????????????(??????Feign?????????????????????????????????)
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        MemberResponseVo memberResponseVo = OrderInterceptor.threadLocal.get();
        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            //1???????????????????????????
            List<MemberAddressVo> address = memberFeignService.getAddress(memberResponseVo.getId());
            //????????????????????????????????????????????????
            RequestContextHolder.setRequestAttributes(requestAttributes);
            confirmVo.setAddress(address);
        }, executor);
        CompletableFuture<Void> getCartFuture = CompletableFuture.runAsync(() -> {
            //2???????????????????????????????????????????????????
//            List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
            //????????????????????????????????????????????????
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
                //???skuStockVos???????????????map
                Map<Long, Boolean> skuHasStockMap = data.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                confirmVo.setStocks(skuHasStockMap);
            }
        }, executor);
        //3.?????????????????????
        Integer integration = memberResponseVo.getIntegration();
        confirmVo.setIntegration(integration);
        CompletableFuture.allOf(getAddressFuture, getCartFuture).get();
        //4.????????????????????????
        //TODO ??????????????????
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId(), token, 30, TimeUnit.MINUTES);
        confirmVo.setOrderToken(token);
        return confirmVo;
    }

    /*
     *  ?????????
     *     1.????????????
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
        //1.???????????? ???redis???????????????[?????????????????????????????????????????????]
        // ???????????? 0 ?????????????????? - 1 ????????????
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        String orderToken = vo.getOrderToken();
        //????????????
        Long result = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId()), orderToken);
        response.setCode(0);
        if (result == 0L) {
            response.setCode(1);   //token????????????
            return response;
        } else {
            //token????????????
            //1.????????????
            OrderCreateTo order = creatOrder();
            //2.????????????
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = vo.getPayPrice();
            if (Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01) { //??????????????????
                //3.????????????
                saveOrder(order);
                //4.?????????????????????????????????????????????
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
                //????????????
                R r = wmsFeignService.orderLockStock(wareSkuLockVo);
                //TODO ??????????????????
                if (r.getCode() == 0) {
                    //??????????????????
                    response.setOrder(order.getOrder());
//                    int i = 10 / 0;
                    //TODO ??????????????????????????????
                    rabbitTemplate.convertAndSend("order-event-exchange","order.create.order",order);
                    return response;
                } else {
                    String msg = (String) r.get("msg");
                    throw new NoStockException(msg);
                }
            } else {
                response.setCode(2);//??????????????????
                return response;
            }
        }
    }

    @Override
    public OrderEntity getOrderStatusBySn(String orderSn) {
        return this.baseMapper.selectOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
    }

     /*
      * ????????????
      * @return
      * @author djx
      * @deprecated: Talk is cheap,show me the code
      * @date 2022/11/28 18:57
      */
    @Override
    public void closeOrder(OrderEntity entity) {

    }

    /*
     * ????????????
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
     * ????????????
     * @return
     * @author djx
     * @deprecated: Talk is cheap,show me the code
     * @date 2022/11/24 14:33
     */
    private OrderCreateTo creatOrder() {
        //1.????????????
        OrderCreateTo createTo = new OrderCreateTo();
        //1.?????????????????????
        String orderSn = IdWorker.getTimeId();
        OrderEntity orderEntity = buildOrder(orderSn);
        //2.???????????????????????????
        List<OrderItemEntity> itemsEntity = buildOrderItems(orderSn);
        //3.??????????????????
        computePrice(orderEntity, itemsEntity);
        //????????????
        createTo.setOrder(orderEntity);
        createTo.setOrderItems(itemsEntity);

        return createTo;
    }

    /*
     * ????????????????????????
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
        //1.?????????????????????
        //??????????????? ????????????????????????
        for (OrderItemEntity entity : itemsEntity) {
            //????????????
            BigDecimal realAmount = entity.getRealAmount();
            totalPrice = totalPrice.add(realAmount);
            coupon = coupon.add(entity.getCouponAmount());
            integrationAmount = integrationAmount.add(entity.getIntegrationAmount());
            promotionAmount = promotionAmount.add(entity.getPromotionAmount());
            giftIntegration = giftIntegration.add(new BigDecimal(entity.getGiftIntegration().toString()));
            giftGrowth = giftGrowth.add(new BigDecimal(entity.getGiftGrowth().toString()));
        }
        //??????
        orderEntity.setTotalAmount(totalPrice);
        //????????????
        orderEntity.setPayAmount(totalPrice.add(orderEntity.getFreightAmount()));
        orderEntity.setPromotionAmount(promotionAmount);
        orderEntity.setIntegrationAmount(integrationAmount);
        orderEntity.setCouponAmount(coupon);
        //????????????????????????
        orderEntity.setGrowth(giftGrowth.intValue());
        orderEntity.setIntegration(giftIntegration.intValue());
        orderEntity.setDeleteStatus(0);


    }

    /*
     * ????????????
     * @return
     * @author djx
     * @deprecated: Talk is cheap,show me the code
     * @date 2022/11/24 15:31
     */
    private OrderEntity buildOrder(String orderSn) {

        MemberResponseVo responseVo = OrderInterceptor.threadLocal.get();
        //?????????????????????
        OrderEntity entity = new OrderEntity();
        entity.setOrderSn(orderSn);
        entity.setMemberId(responseVo.getId());
//        entity.setStatus();
        //1.??????????????????
        OrderSubmitVo submitVo = confirmVoThreadLocal.get();
        //?????????????????????????????????
        R fare = wmsFeignService.getFare(submitVo.getAddrId());
        FareVo fareResp = fare.getData(new TypeReference<FareVo>() {
        });
        //??????????????????
        entity.setFreightAmount(fareResp.getFare());
        //?????????????????????   ????????????
        entity.setReceiverCity(fareResp.getAddress().getCity());
        //???????????????
        entity.setReceiverDetailAddress(fareResp.getAddress().getDetailAddress());
        //?????????????????????
        entity.setReceiverName(fareResp.getAddress().getName());
        //?????????
        entity.setReceiverPhone(fareResp.getAddress().getPhone());
        //???????????????
        entity.setReceiverPostCode(fareResp.getAddress().getPostCode());
        //??????/?????????
        entity.setReceiverProvince(fareResp.getAddress().getProvince());
        // ???
        entity.setReceiverRegion(fareResp.getAddress().getRegion());
        //??????????????????????????????
        entity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        //??????????????????
        entity.setAutoConfirmDay(7);

        return entity;
    }

    /*
     * ???????????????????????????
     * @return
     * @author djx
     * @deprecated: Talk is cheap,show me the code
     * @date 2022/11/24 15:24
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        //2.??????????????????????????????
        R r = cartFeignService.getCurrentUserCartItems();
        List<OrderItemVo> data = r.getData(new TypeReference<List<OrderItemVo>>() {
        });
        if (data != null && data.size() > 0) {
            //??????????????????????????????
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
     * ??????????????????????????????
     * @return
     * @author djx
     * @deprecated: Talk is cheap,show me the code
     * @date 2022/11/24 15:29
     */
    private OrderItemEntity buildOrderItem(OrderItemVo cartItem) {
        OrderItemEntity orderItemEntity = new OrderItemEntity();
        //1.???????????? ?????????
        //2.?????????spu
        Long skuId = cartItem.getSkuId();
        R r = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo data = r.getData(new TypeReference<SpuInfoVo>() {
        });
        orderItemEntity.setSpuId(data.getId());
        orderItemEntity.setSpuName(data.getSpuName());
        orderItemEntity.setSpuBrand(data.getBrandId().toString());
        orderItemEntity.setCategoryId(data.getCatalogId());
        //3.?????????sku
        orderItemEntity.setSkuId(cartItem.getSkuId());
        List<String> attr = cartItem.getSkuAttr();
        String skuAttrValues = StringUtils.collectionToDelimitedString(attr, ";");
        orderItemEntity.setSkuAttrsVals(skuAttrValues);
        orderItemEntity.setSkuName(cartItem.getTitle());
        orderItemEntity.setSkuPic(cartItem.getImages());
        orderItemEntity.setSkuPrice(cartItem.getPrice());
        orderItemEntity.setSkuQuantity(cartItem.getCount());
        //4.??????
        //5.????????????
        orderItemEntity.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
        orderItemEntity.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());

        //6.????????????????????????
        orderItemEntity.setPromotionAmount(new BigDecimal("0"));
        orderItemEntity.setCouponAmount(new BigDecimal("0"));
        orderItemEntity.setIntegrationAmount(new BigDecimal("0"));
        //????????????????????????
        //??????????????? ???????????????????????????
        BigDecimal orign = orderItemEntity.getSkuPrice().multiply(new BigDecimal(orderItemEntity.getSkuQuantity().toString()));
        BigDecimal subtract = orign.subtract(orderItemEntity.getCouponAmount()).subtract(orderItemEntity.getPromotionAmount()).subtract(orderItemEntity.getIntegrationAmount());
        orderItemEntity.setRealAmount(subtract);
        return orderItemEntity;
    }

}