package com.atguigu.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.exception.NoStockException;
import com.atguigu.common.to.mq.OrderTo;
import com.atguigu.common.to.mq.StockDetailTo;
import com.atguigu.common.to.mq.StockLockedTo;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.atguigu.gulimall.ware.entity.WareOrderTaskEntity;
import com.atguigu.gulimall.ware.feign.OrderFeignService;
import com.atguigu.gulimall.ware.feign.ProductFeignService;
import com.atguigu.gulimall.ware.service.WareOrderTaskService;
import com.atguigu.gulimall.ware.vo.OrderItemVo;
import com.atguigu.gulimall.ware.vo.OrderVo;
import com.atguigu.gulimall.ware.vo.SkuHasStockVo;
import com.atguigu.gulimall.ware.vo.WareSkuLockVo;
import com.rabbitmq.client.Channel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.ware.dao.WareSkuDao;
import com.atguigu.gulimall.ware.entity.WareSkuEntity;
import com.atguigu.gulimall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;


@Service("wareSkuService")
@Slf4j
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {


    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private WareOrderTaskDetailServiceImpl orderTaskDetailService;

    @Autowired
    private WareOrderTaskService orderTaskService;

    @Autowired
    private OrderFeignService orderFeignService;


    /*
     * 解锁库存
     * @return
     * @author djx
     * @deprecated: Talk is cheap,show me the code
     * @date 2022/11/28 16:34
     */
    private void unLockStock(Long skuId, Long wareId, Integer num, Long taskDetailId) {
        //库存解锁
        this.baseMapper.unLockStock(skuId, wareId, num);
        //库存解锁成功更新工作单状态
        WareOrderTaskDetailEntity detailEntity = new WareOrderTaskDetailEntity();
        detailEntity.setId(taskDetailId);
        detailEntity.setLockStatus(2);
        orderTaskDetailService.updateById(detailEntity);
    }

    /**
     * {
     * wareId: 123,//仓库id
     * skuId: 123//商品id
     * }
     *
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    /**
     * 新增库存的时候如果没有库存就新增一条记录 如果有就修改库存编号和sku编号
     *
     * @param skuNum
     * @param skuId
     * @param wareId
     */
    @Override
    public void addStock(Integer skuNum, Long skuId, Long wareId) {
        List<WareSkuEntity> entities = this.list(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId));
        if (entities == null || entities.size() == 0) {
            WareSkuEntity wareSkuEntity = new WareSkuEntity();
            wareSkuEntity.setSkuId(skuId);
            wareSkuEntity.setWareId(wareId);
            wareSkuEntity.setStock(skuNum);
            wareSkuEntity.setStockLocked(0);
            //TODO 远程调用设置sku的名字，如果失败事务无需回滚
            try {
                R info = productFeignService.info(skuId);
                Map<String, Object> data = (Map<String, Object>) info.get("skuInfo");
                if (info.getCode() == 0) {
                    wareSkuEntity.setSkuName((String) data.get("skuName"));
                }
            } catch (Exception e) {

            } finally {

            }
            this.baseMapper.insert(wareSkuEntity);
        } else {
            this.baseMapper.addStock(skuNum, skuId, wareId);
        }
    }

    @Override
    public List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds) {
        List<SkuHasStockVo> skuHasStockVos = skuIds.stream().map(item -> {
            SkuHasStockVo vo = new SkuHasStockVo();
            //查询当前库存的总库存
            Long count = this.baseMapper.getSkuStock(item);
            vo.setSkuId(item);
            vo.setHasStock(count == null ? false : count > 0);
            return vo;
        }).collect(Collectors.toList());
        return skuHasStockVos;
    }


    /*
     * 为某个订单锁定库存
     * @return
     * @author djx
     * @deprecated: Talk is cheap,show me the code
     * @date 2022/11/24 19:32
     */
    @Transactional(rollbackFor = NoStockException.class)
    @Override
    public Boolean orderLockStock(WareSkuLockVo vo) {
        //1.锁定库存
        /**
         * 追溯订单消息
         */
        WareOrderTaskEntity orderTaskEntity = new WareOrderTaskEntity();
        orderTaskEntity.setOrderSn(vo.getOrderSn());
        orderTaskService.save(orderTaskEntity);
        //按照下单的收获地址，找到就近创库，锁定库存
        //1.找到每个商品在哪个创库有库存
        List<OrderItemVo> locks = vo.getLocks();
        //找到有库存的sku
        List<SkuWareHasStock> wareHasStocks = locks.stream().map(item -> {
            Long skuId = item.getSkuId();
            SkuWareHasStock stock = new SkuWareHasStock();
            stock.setSkuId(skuId);
            stock.setNum(item.getCount());
            //查询这个商品在哪个仓库有库存
            List<Long> wareId = this.baseMapper.listWareIdHasSkuStock(skuId);
            stock.setWareId(wareId);
            return stock;
        }).collect(Collectors.toList());
        for (SkuWareHasStock hasStock : wareHasStocks) {
            Boolean skuStock = false;
            Long skuId = hasStock.getSkuId();
            List<Long> wareIds = hasStock.getWareId();
            //没有仓库库存
            if (wareIds == null || wareIds.size() == 0) {
                throw new NoStockException(skuId);
            }
            //1.锁定成功，将当前商品锁定了几件，工作单记录发送给MQ
            //2.锁定失败 前面保存的工作单消息就回滚了、发送出去的消息也没问题，由于查不到id
            for (Long wareId : wareIds) {
                //锁定库存c成功返回1 返回1
                Long count = this.baseMapper.lockSkuStock(skuId, wareId, hasStock.getNum());
                if (count == 1) {
                    //锁定成功
                    skuStock = true;
                    //TODO 注入MQ发送消息库存锁定成功
                    R info = productFeignService.info(skuId);
                    Map<String, Object> data = (Map<String, Object>) info.get("skuInfo");
                    WareOrderTaskDetailEntity orderTaskDetailEntity = new WareOrderTaskDetailEntity(null, skuId, (String) data.get("skuName"), hasStock.getNum(), orderTaskEntity.getId(), wareId, 1);
                    orderTaskDetailService.save(orderTaskDetailEntity);
                    //构造mq库存锁定成功的工作单
                    StockLockedTo lockedTo = new StockLockedTo();
                    lockedTo.setId(orderTaskEntity.getId());
                    StockDetailTo detailTo = new StockDetailTo();
                    BeanUtils.copyProperties(orderTaskDetailEntity, detailTo);
                    //防止回滚找不到数据
                    lockedTo.setDetail(detailTo);
                    rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", lockedTo);
                    log.info("库存锁定成功。成功向队列推送消息:stock.delay.queue");
                    break;
                } else {
                    //当前仓库锁定失败尝试锁定下一个仓库
                }
            }
            if (skuStock == false) {
                //当前商品所有仓库都没锁定
                throw new NoStockException(skuId);
            }
        }
        //3.全部都是锁定成功
        return true;
    }

    @Override
    public void unlockStock(StockLockedTo to) {
        //库存工作单的id
        StockDetailTo detail = to.getDetail();
        Long detailId = detail.getId();
        /**
         * 解锁
         * 1、查询数据库关于这个订单锁定库存信息
         *   有：证明库存锁定成功了
         *      解锁：订单状况
         *          1、没有这个订单，必须解锁库存
         *          2、有这个订单，不一定解锁库存
         *              订单状态：已取消：解锁库存
         *                      已支付：不能解锁库存
         */
        WareOrderTaskDetailEntity taskDetailInfo = orderTaskDetailService.getById(detailId);
        if (taskDetailInfo != null) {
            //查出wms_ware_order_task工作单的信息
            Long id = to.getId();
            WareOrderTaskEntity orderTaskInfo = orderTaskService.getById(id);
            //获取订单号查询订单状态
            String orderSn = orderTaskInfo.getOrderSn();
            //远程查询订单信息
            R orderData = orderFeignService.getOrderStatusBySn(orderSn);
            if (orderData.getCode() == 0) {
                //订单数据返回成功
                OrderVo orderInfo = orderData.getData("data", new TypeReference<OrderVo>() {
                });
                //判断订单状态是否已取消或者支付或者订单不存在
                if (orderInfo == null || orderInfo.getStatus() == 4) {
                    //订单已被取消，才能解锁库存
                    if (taskDetailInfo.getLockStatus() == 1) {
                        //当前库存工作单详情状态1，已锁定，但是未解锁才可以解锁
                        unLockStock(detail.getSkuId(), detail.getWareId(), detail.getSkuNum(), detailId);
                    }
                }
            } else {
                //消息拒绝以后重新放在队列里面，让别人继续消费解锁
                //远程调用服务失败
                throw new RuntimeException("远程调用服务失败");
            }
        } else {
            //无需解锁
        }
    }

    /*
     * 防止订单卡顿，导致订单状态消息一直不能修改，库存消息优先到齐，查订单状态无法正确解锁库存
     * @return
     * @author djx
     * @deprecated: Talk is cheap,show me the code
     * @date 2022/11/28 20:22
     */
    @Transactional
    @Override
    public void orderLockStock(OrderTo orderTo) {
        String orderSn = orderTo.getOrderSn();
        //查询订单最新状态
        R r = orderFeignService.getOrderStatusBySn(orderSn);
        WareOrderTaskEntity task = orderTaskService.getOrderTaskByOrderSn(orderSn);
        //工作单id
        Long taskId = task.getId();
        //按照库存工作单查找所有没有解锁的库存进行解锁
        List<WareOrderTaskDetailEntity> detailByTaskId =
                orderTaskDetailService.getOrderTaskDetailByTaskId(taskId);
        //解锁
        for (WareOrderTaskDetailEntity detailEntity : detailByTaskId) {
//            Long skuId, Long wareId, Integer num, Long taskDetailId
            //解锁库存
            unLockStock(detailEntity.getSkuId(), detailEntity.getWareId(), detailEntity.getSkuNum(), detailEntity.getId());
        }
    }

    @Data
    class SkuWareHasStock {
        private Long skuId;
        private Integer num;
        private List<Long> wareId;
    }

}