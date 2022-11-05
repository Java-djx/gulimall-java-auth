package com.atguigu.gulimall.product.service.impl;

import com.atguigu.gulimall.product.vo.SpuSaveVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.SpuInfoDao;
import com.atguigu.gulimall.product.entity.SpuInfoEntity;
import com.atguigu.gulimall.product.service.SpuInfoService;
import org.springframework.transaction.annotation.Transactional;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

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
     *
     * @param saveVo
     */
    @Transactional
    @Override
    public void saveSpuInfo(SpuSaveVo saveVo) {

        //1.保存SPU基本消息 pms_spu_info
        SpuInfoEntity spuInfoEntity = new SpuInfoEntity();
        //属性对拷
        BeanUtils.copyProperties(saveVo,spuInfoEntity);

        this.saveBaseSpkInfo(spuInfoEntity);

        //2.保存SPU描述消息 pms_spu_info_desc

        //3.保存SPU图片集 pms_spu_images


        //4.保存spu的规格参数 pms_product_attr_value

        //保存spu的积分消息 gulimall_sms-》sms_spu_bounds


        //5. 保存当前SPU对应的sku消息
        //5.1)sku的基本消息    pms_sku_info
        //5.2)sku的图片消息    pms_sku_images
        //5.3)sku的销售属性消息 pms_sku_sale_attr_value
        //5.4)sku的优惠 满减 消息 gulimall_sms
        // ->sms_sku_ladder 优惠
        // ->sms_sku_full_reduction 满减
        // ->sms_member_price 会员打折


    }

    /**
     * 保存SPU 基本消息
     * @param spuInfoEntity
     */
    @Override
    public void saveBaseSpkInfo(SpuInfoEntity spuInfoEntity) {
        this.baseMapper.insert(spuInfoEntity);
    }

}