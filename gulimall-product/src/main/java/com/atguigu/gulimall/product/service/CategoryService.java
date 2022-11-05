package com.atguigu.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.CategoryEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品三级分类
 *
 * @author djx
 * @email djx@gmail.com
 * @date 2022-11-02 14:52:01
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 查询分类树形菜单
     *
     * @return
     */
    List<CategoryEntity> listWithTree();

    /**
     * 删除树形菜单
     *
     * @param asList
     */
    void removeMenuByIds(List<Long> asList);

    /**
     * 找到 catelogId 完整路径
     * [父/子/孙]
     *
     * @param catelogId
     * @return
     */
    Long[] findCatelogPath(Long catelogId);

    /**
     * 修改树形菜单
     *
     * @param category
     */
    void updateByCascade(CategoryEntity category);
}

