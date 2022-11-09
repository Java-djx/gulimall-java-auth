package com.atguigu.gulimall.product.dao;

import com.atguigu.gulimall.product.entity.AttrEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品属性
 * 
 * @author djx
 * @email djx@gmail.com
 * @date 2022-11-02 14:52:01
 */
@Mapper
public interface AttrDao extends BaseMapper<AttrEntity> {

    /**
     * 所有可以被检索的属性
     * @param attrIds
     * @return
     */
    List<Long> selectSearchAttrIds(@Param("attrIds") List<Long> attrIds);

}
