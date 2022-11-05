package com.atguigu.gulimall.product.service.impl;

import com.atguigu.common.to.SkuReductionTo;
import com.atguigu.common.to.SpuBoundTo;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.product.entity.*;
import com.atguigu.gulimall.product.feign.CouponFeignService;
import com.atguigu.gulimall.product.service.*;
import com.atguigu.gulimall.product.vo.*;
import com.sun.org.apache.bcel.internal.generic.NEW;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Transactional;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Autowired
    private SpuInfoDescService descService;

    @Autowired
    private SpuImagesService imagesService;

    @Autowired
    private AttrService attrService;

    @Autowired
    private ProductAttrValueService attrValueService;

    @Autowired
    private SkuInfoService skuInfoService;

    @Autowired
    private SkuImagesService skuImagesService;

    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;

    //远程调用优惠券服务
    @Autowired
    private CouponFeignService couponFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 保存 SPU基本消息 SPU图片 SPU详情
     * TODO 等待分布式事务解决问题
     *
     * @param vo
     */
    @Transactional
    @Override
    public void saveSpuInfo(SpuSaveVo vo) {
        //1.保存SPU基本消息 pms_spu_info
        SpuInfoEntity spuInfoEntity = new SpuInfoEntity();
        //属性对拷
        BeanUtils.copyProperties(vo, spuInfoEntity);
        spuInfoEntity.setCreateTime(new Date());
        spuInfoEntity.setUpdateTime(new Date());
        this.saveBaseSpkInfo(spuInfoEntity);
        //2.保存SPU描述消息 pms_spu_info_desc
        List<String> decript = vo.getDecript();
        SpuInfoDescEntity descEntity = new SpuInfoDescEntity();
        descEntity.setSpuId(spuInfoEntity.getId());
        descEntity.setDecript(String.join(",", decript));
        descService.saveDescEntity(descEntity);
        //3.保存SPU图片集 pms_spu_images
        List<String> images = vo.getImages();
        imagesService.saveImages(spuInfoEntity.getId(), images);
        //4.保存spu的规格参数 pms_product_attr_value
        List<BaseAttrs> baseAttrs = vo.getBaseAttrs();
        List<ProductAttrValueEntity> valueEntityList = baseAttrs.stream()
                .map(item -> {
                    ProductAttrValueEntity valueEntity = new ProductAttrValueEntity();
                    valueEntity.setSpuId(spuInfoEntity.getId());
                    valueEntity.setAttrId(item.getAttrId());
                    //根据属性名查出对应的属性值
                    AttrEntity entity = attrService.getById(item.getAttrId());
                    valueEntity.setAttrName(entity.getAttrName() == null ? "" : entity.getAttrName());
                    valueEntity.setAttrValue(item.getAttrValues());
                    valueEntity.setQuickShow(item.getShowDesc());
                    return valueEntity;
                }).collect(Collectors.toList());
        //插入数据
        attrValueService.saveBatchProductAttr(valueEntityList);
        //保存spu的积分消息 gulimall_sms-》sms_spu_bounds
        Bounds bounds = vo.getBounds();
        //构造远程传输对象TO
        SpuBoundTo spuBoundTo = new SpuBoundTo();
        BeanUtils.copyProperties(bounds, spuBoundTo);
        spuBoundTo.setSpuId(spuInfoEntity.getId());
        //保存spu的积分消息  调用优惠券微服务
        R r = couponFeignService.saveSpuBounds(spuBoundTo);
        if (r.getCode() != 0) {
            log.error("远程服务调用spu积分消息失败");
        }

        //5. 保存当前SPU对应的sku消息
        //5.1)sku的基本消息    pms_sku_info
        List<Skus> skus = vo.getSkus();
        if (skus != null && skus.size() > 0) {
            skus.forEach(item -> {
                /**
                 * 收集图片消息
                 */
                String defaultImg = "";
                for (Images image : item.getImages()) {
                    if (image.getDefaultImg() == 1) {
                        defaultImg = image.getImgUrl();
                    }
                }
                //收集消息
                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(item, skuInfoEntity);
                skuInfoEntity.setBrandId(spuInfoEntity.getBrandId());
                skuInfoEntity.setCatalogId(spuInfoEntity.getCatalogId());
                skuInfoEntity.setSaleCount(0L);
                skuInfoEntity.setSpuId(spuInfoEntity.getId());
                skuInfoEntity.setSkuDefaultImg(defaultImg);
                skuInfoService.saveSkuInfo(skuInfoEntity);
                //获取sku自增id
                Long skuId = skuInfoEntity.getSkuId();
                List<SkuImagesEntity> imagesEntities = item.getImages().stream().map(img -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setImgUrl(img.getImgUrl());
                    skuImagesEntity.setDefaultImg(img.getDefaultImg());
                    return skuImagesEntity;
                }).filter(entitys -> {
                    //返回true就是需要的属性
                    return !StringUtils.isBlank((entitys.getImgUrl()));
                }).collect(Collectors.toList());
                //5.2)sku的图片消息    pms_sku_images
                //TODO 没有图片路径不保存
                skuImagesService.saveBatch(imagesEntities);
                //5.3)sku的销售属性消息 pms_sku_sale_attr_value
                List<Attr> attr = item.getAttr();
                //绑定sku的图片消息
                List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = attr.stream().map(irrs -> {
                    SkuSaleAttrValueEntity skuSaleAttrValueEntity = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(irrs, skuSaleAttrValueEntity);
                    skuSaleAttrValueEntity.setSkuId(skuId);
                    return skuSaleAttrValueEntity;
                }).collect(Collectors.toList());
                //保存销售和sku的消息
                skuSaleAttrValueService.saveBatch(skuSaleAttrValueEntities);

                //5.4)sku的优惠 满减 消息 gulimall_sms
                // ->sms_sku_ladder 优惠
                // ->sms_sku_full_reduction 满减
                // ->sms_member_price 会员打折
                SkuReductionTo skuReductionTo = new SkuReductionTo();
                BeanUtils.copyProperties(item, skuReductionTo);
                skuReductionTo.setSkuId(skuId);
                if (skuReductionTo.getFullCount() > 0 || skuReductionTo.getFullPrice().compareTo(new BigDecimal("0")) == 1) {
                    R r1 = couponFeignService.saveInfo(skuReductionTo);
                    if (r1.getCode() != 0) {
                        log.error("远程服务调用spu优惠失败");
                    }
                }

            });
        }


    }

    /**
     * 保存SPU 基本消息
     *
     * @param spuInfoEntity
     */
    @Override
    public void saveBaseSpkInfo(SpuInfoEntity spuInfoEntity) {
        this.baseMapper.insert(spuInfoEntity);
    }


}