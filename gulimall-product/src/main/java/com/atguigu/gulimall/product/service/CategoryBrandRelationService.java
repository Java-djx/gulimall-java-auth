package com.atguigu.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.CategoryBrandRelationEntity;

import java.util.Map;

/**
 * 品牌分类关联
 *
 * @author djx
 * @email djx@gmail.com
 * @date 2022-11-02 14:52:00
 */
public interface CategoryBrandRelationService extends IService<CategoryBrandRelationEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 保存品牌和分类关联
     * @param categoryBrandRelation
     */
    void saveDetail(CategoryBrandRelationEntity categoryBrandRelation);

    /**
     * 修改品牌冗余字段
     * @param brandId
     * @param name
     */
    void updateBrand(Long brandId, String name);

    /**
     * 修改分类冗余字段
     * @param catId
     * @param name
     */
    void updateCategory(Long catId, String name);
}

