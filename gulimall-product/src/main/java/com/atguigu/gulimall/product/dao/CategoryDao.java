package com.atguigu.gulimall.product.dao;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author djx
 * @email djx@gmail.com
 * @date 2022-11-02 14:52:01
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
