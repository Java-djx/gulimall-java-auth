package com.atguigu.gulimall.product.vo;

import com.atguigu.gulimall.product.entity.SkuImagesEntity;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;
import com.atguigu.gulimall.product.entity.SpuInfoDescEntity;
import lombok.Data;

import java.util.List;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/15 19:53
 */
@Data
public class SkuItemVo {

    //1、获取sku基本消息 pms_sku_info
    private SkuInfoEntity info;
    //2、获取sku图片消息 pms_sku_images
    private List<SkuImagesEntity> images;
    //3、获取spu销售属性组合
    private List<SkuItemSaleAttrsVo> saleAttr;
    //4、获取spu的介绍
    private SpuInfoDescEntity desp;
    //5、获取spu的规格参数
    private List<SpuItemAttrGroupVo> attrGroupAttrs;

    //有货无货
    private Boolean hasStock=true;






}
