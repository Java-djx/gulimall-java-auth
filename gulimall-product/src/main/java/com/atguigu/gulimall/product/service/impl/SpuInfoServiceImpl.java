package com.atguigu.gulimall.product.service.impl;

import com.atguigu.common.constant.ProductConstant;
import com.atguigu.common.to.SkuReductionTo;
import com.atguigu.common.to.SpuBoundTo;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.product.entity.*;
import com.atguigu.gulimall.product.feign.CouponFeignService;
import com.atguigu.gulimall.product.feign.SearchFeignService;
import com.atguigu.gulimall.product.feign.WareFeignService;
import com.atguigu.gulimall.product.service.*;
import com.atguigu.gulimall.product.vo.*;
import com.sun.org.apache.bcel.internal.generic.NEW;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Transactional;


@Service("spuInfoService")
@Slf4j
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    //商品详情服务
    @Autowired
    private SpuInfoDescService descService;

    //商品图片服务
    @Autowired
    private SpuImagesService imagesService;

    //属性服务
    @Autowired
    private AttrService attrService;

    //商品和属性关联服务
    @Autowired
    private ProductAttrValueService attrValueService;

    //sku详情服务
    @Autowired
    private SkuInfoService skuInfoService;

    //sku图片服务
    @Autowired
    private SkuImagesService skuImagesService;

    //sku和属性关联服务
    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;


    //品牌
    @Autowired
    private BrandService brandService;

    @Autowired
    private CategoryService categoryService;

    //远程调用优惠券服务
    @Autowired
    private CouponFeignService couponFeignService;

    //远程调用库存服务
    @Autowired
    private WareFeignService wareFeignService;

    //远程调用ES服务保存商品
    @Autowired
    private SearchFeignService searchFeignService;

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
                List<SkuImagesEntity> imagesEntities = item.getImages()
                        .stream().map(img -> {
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

    /**
     * spu检索
     * {
     * key: '华为',//检索关键字
     * catelogId: 6,//三级分类id
     * brandId: 1,//品牌id
     * status: 0,//商品状态
     * }
     *
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {

        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        if (!StringUtils.isBlank(key)) {
            wrapper.and((w -> {
                w.eq("id", key).or().like("spu_name", key);
            }));
        }
        String status = (String) params.get("status");
        if (!StringUtils.isBlank(status)) {
            wrapper.eq("publish_status", status);
        }
        String brandId = (String) params.get("brandId");
        if (!StringUtils.isBlank(brandId) && !brandId.equals("0")) {
            wrapper.eq("brand_id", brandId);
        }
        String catelogId = (String) params.get("catelogId");
        if (!StringUtils.isBlank(catelogId) && !catelogId.equals("0")) {
            wrapper.eq("catalog_id", catelogId);
        }

        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    /**
     * 商品上架  购物ES数据模型
     *
     * @param spuId
     */
    @Override
    public void up(Long spuId) {
        List<SkuEsModel> upProducts = new ArrayList<SkuEsModel>();

        //2.查出当前spuId对应的sku消息，还有品牌消息
        List<SkuInfoEntity> skus = skuInfoService.getSkusBySpuId(spuId);

        //筛选当前skuId
        List<Long> skuIds = skus.stream().map(item -> {
            return item.getSkuId();
        }).collect(Collectors.toList());

        Map<Long, Boolean> stockMap = null;
        try {
            //4, TODO 查询当前SKU可以检索规格属性
            R<List<SkuHasStockVo>> stocks = wareFeignService.hasStock(skuIds);
            //筛选收据
            List<SkuHasStockVo> date = stocks.getDate();
            stockMap = date.stream().collect(Collectors.toMap(SkuHasStockVo::getSkuId, item -> item.getHasStock()));
        } catch (Exception e) {
            log.error("库存服务查询异常,原因:{}", e);
        }

        //查询全部
        List<ProductAttrValueEntity> baseAttrs = attrValueService.baseAttrByProductId(spuId);
        //筛选数据全部属性id 属性id查询可以被检索的属性
        List<Long> attrIds = baseAttrs.stream().map(item -> {
            return item.getAttrId();
        }).collect(Collectors.toList());
        //可以被检索的属性
        List<Long> searchAttrs = attrService.selectSearchAttrIds(attrIds);
        Set<Long> idSet = new HashSet<>(searchAttrs);
        //筛选
        List<SkuEsModel.Attrs> attrsList = baseAttrs.stream().filter(item -> {
            return idSet.contains(item.getAttrId());
        }).map(item -> {
            SkuEsModel.Attrs attrs = new SkuEsModel.Attrs();
            BeanUtils.copyProperties(item, attrIds);
            return attrs;
        }).collect(Collectors.toList());

        //组装数据
        Map<Long, Boolean> finalStockMap = stockMap;
        List<SkuEsModel> esModelList = skus.stream().map(item -> {
            //1.组装需要的数据
            SkuEsModel esModel = new SkuEsModel();
            //处理属性
            BeanUtils.copyProperties(item, esModel);
            //    price skuDefaultImg
            esModel.setSkuPrice(item.getPrice());
            esModel.setSkuImg(item.getSkuDefaultImg());
            //hasStock hotScore
            //1. TODO 发送远程调用库存系统查询是否存在库存
            //在map中寻找数据 对于是否有库存
            if (finalStockMap == null) {
                esModel.setHasStock(true);
            } else {
                esModel.setHasStock(finalStockMap.get(item.getSkuId()));
            }

            //2. TODO 2,热度评分.0
            esModel.setHotScore(0L);

            //3, TODO 查询品牌和分类的消息
            BrandEntity brand = brandService.getById(esModel.getBrandId());
            esModel.setBrandName(brand.getName());
            esModel.setBrandImg(brand.getLogo());
            CategoryEntity category = categoryService.getById(esModel.getCatalogId());
            esModel.setCatalogName(category.getName());
            // 设置对应的属性规格消息
            esModel.setAttrs(attrsList);
            return esModel;
        }).collect(Collectors.toList());

        //TODO 发送给ES进行保存 属性模型 gulimall-search 保存
        try {
            R r = searchFeignService.productStatusUp(upProducts);
            if (r.getCode() == 0) {
                log.info("远程调用ES服务保存商品成功");
                //TODO 6、ES保存成功改变当前SPU发布状态 已上架
                this.baseMapper.updateSpuStatus(spuId, ProductConstant.StatusEnum.SPU_UP.getCode());
            } else {
                log.error("远程调用ES服务保存商品失败");
                //TODO 重复调用？ 接口幂等性 重试机制！
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }


}