package com.atguigu.gulimall.product.service;

import com.atguigu.gulimall.product.vo.AttrGroupRelationVo;
import com.atguigu.gulimall.product.vo.AttrRespVo;
import com.atguigu.gulimall.product.vo.AttrVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.AttrEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品属性
 *
 * @author djx
 * @email djx@gmail.com
 * @date 2022-11-02 14:52:01
 */
public interface AttrService extends IService<AttrEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 新增属性
     * @param attr
     */
    void saveAttr(AttrVo attr);

    /**
     * 获取属性列表
     * @param params
     * @param catelogId
     * @param type
     * @return
     */
    PageUtils queryBaseAtrrPage(Map<String, Object> params, Long catelogId, String type);

    /**
     * 获取属性单个详情
     * @param attrId
     * @return
     */
    AttrRespVo getAttrInfo(Long attrId);

    /**
     * 修改属性
     * @param attr
     */
    void updateAttr(AttrVo attr);

    /**
     * 根据分组id找到组内的所有属性
     * @param attrgroupId
     * @return
     */
    List<AttrEntity> getRelationAttr(Long attrgroupId);

    /**
     * 删除属性与分组的关联关系
     * @param vos
     */
    void deleteRelation(AttrGroupRelationVo[] vos);

    /**
     * 获取没有分组的属性
     * @param params
     * @param attrgroupId
     * @return
     */
    PageUtils getNoRelationAttr(Map<String, Object> params, Long attrgroupId);

    /**
     *在指定所有属性集合挑出检索属性
     * @param attrIds
     * @return
     */
    List<Long> selectSearchAttrIds(List<Long> attrIds);
}

