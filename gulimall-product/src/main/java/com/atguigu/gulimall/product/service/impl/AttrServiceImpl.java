package com.atguigu.gulimall.product.service.impl;

import com.atguigu.common.constant.ProductConstant;
import com.atguigu.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.atguigu.gulimall.product.dao.AttrGroupDao;
import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.atguigu.gulimall.product.entity.AttrGroupEntity;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.AttrGroupRelationVo;
import com.atguigu.gulimall.product.vo.AttrRespVo;
import com.atguigu.gulimall.product.vo.AttrVo;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.AttrDao;
import com.atguigu.gulimall.product.entity.AttrEntity;
import com.atguigu.gulimall.product.service.AttrService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;


@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {

    @Resource
    private AttrAttrgroupRelationDao attrAttrgroupRelationDao;

    @Resource
    private AttrGroupDao attrGroupDao;

    @Resource
    private CategoryDao categoryDao;

    @Autowired
    private CategoryService categoryService;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void saveAttr(AttrVo attr) {
        AttrEntity attrEntity = new AttrEntity();
        //复制属性  来源  --> 给谁
        BeanUtils.copyProperties(attr, attrEntity);
        //保存基本数据
        this.save(attrEntity);
        //保存关联关系
        if (attr.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode() && attr.getAttrGroupId() != null) {
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrGroupId(attr.getAttrGroupId());
            relationEntity.setAttrId(attrEntity.getAttrId());
            attrAttrgroupRelationDao.insert(relationEntity);
        }
    }

    @Override
    public PageUtils queryBaseAtrrPage(Map<String, Object> params, Long catelogId, String type) {

        QueryWrapper<AttrEntity> queryWrapper = new QueryWrapper<AttrEntity>()
                .eq("attr_type", "base".equalsIgnoreCase(type) ? ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode() : ProductConstant.AttrEnum.ATTR_TYPE_SALE.getCode());

        if (catelogId != 0) {
            queryWrapper.eq("catelog_id", catelogId);
        }
        String key = (String) params.get("key");

        if (!StringUtils.isEmpty(key)) {
            //attr_id attr_name
            queryWrapper.and((wrapper) -> {
                wrapper.eq("attr_id", key).or().like("attr_name", key);
            });
        }

        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                queryWrapper
        );

        PageUtils pageUtils = new PageUtils(page);

        //得到查询的数据 重新处理数据
        List<AttrEntity> records = page.getRecords();

        //重新处理集合 吧每一项都重新处理
        List<AttrRespVo> respVos = records.stream().map((attrEntity) -> {
            AttrRespVo attrRespVo = new AttrRespVo();
            BeanUtils.copyProperties(attrEntity, attrRespVo);
            //1.设置分类和分组的名字
            //2 根据属性的id查询当前分组的id
            if ("base".equalsIgnoreCase(type)) {
                AttrAttrgroupRelationEntity relationEntity = attrAttrgroupRelationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrEntity.getAttrId()));
                //2.1根据组id 查询分组消息
                if (relationEntity != null && relationEntity.getAttrGroupId() != null) {
                    AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(relationEntity.getAttrGroupId());
                    //1.设置分组的名字
                    attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
                    attrRespVo.setAttrGroupId(relationEntity.getAttrGroupId());
                }
            }
            //根据分类id 查询分类消息
            CategoryEntity categoryEntity = categoryDao.selectById(attrEntity.getCatelogId());
            if (categoryEntity != null) {
                //设置分类名称
                attrRespVo.setCatelogName(categoryEntity.getName());
            }
            return attrRespVo;
        }).collect(Collectors.toList());

        //重新设置结果
        pageUtils.setList(respVos);

        return pageUtils;
    }

    @Cacheable(value = "attr", key = "'attrInfo:'+#root.args[0 ]")
    @Override
    public AttrRespVo getAttrInfo(Long attrId) {

        AttrRespVo attrRespVo = new AttrRespVo();
        //属性表
        AttrEntity attrEntity = this.getById(attrId);
        //copy
        BeanUtils.copyProperties(attrEntity, attrRespVo);

        /**
         * 分组消息的设置
         */
        if (attrRespVo.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()) {
            AttrAttrgroupRelationEntity relationEntity = attrAttrgroupRelationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrEntity.getAttrId()));
            if (relationEntity != null) {
                //设置分组id
                attrRespVo.setAttrGroupId(relationEntity.getAttrGroupId());
                //查询分组名字
                AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(relationEntity.getAttrGroupId());
                if (attrGroupEntity != null) {
                    attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
                }
            }
        }
        //或者分类id
        Long catelogId = attrEntity.getCatelogId();
        //查询分类路径
        Long[] catelogPath = categoryService.findCatelogPath(catelogId);
        //设置分类消息
        attrRespVo.setCatelogPath(catelogPath);

        CategoryEntity category = categoryService.getById(catelogId);

        if (category != null) {
            //分类的名字
            attrRespVo.setCatelogName(category.getName());
        }

        return attrRespVo;
    }

    @Transactional
    @Override
    public void updateAttr(AttrVo attr) {
        AttrEntity attrEntity = new AttrEntity();
        //复制属性  来源  --> 给谁
        BeanUtils.copyProperties(attr, attrEntity);
        //修改本身的属性
        this.updateById(attrEntity);

        if (attr.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()) {
            //以下操作针对于关联关系
            QueryWrapper<AttrAttrgroupRelationEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("attr_id", attrEntity.getAttrId());
            Integer count = attrAttrgroupRelationDao.selectCount(queryWrapper);
            //关联表中如果存在关联关系就修改 否则就新增关联关系
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            //设置分类id
            relationEntity.setAttrId(attrEntity.getAttrId());
            //设置分组id
            relationEntity.setAttrGroupId(attr.getAttrGroupId());
            if (count > 0) {
                //构造修改条件
                //update pms_attr_attrgroup_relation set attr_group_id=? where attr_id=?
                UpdateWrapper<AttrAttrgroupRelationEntity> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("attr_id", attrEntity.getAttrId());
                attrAttrgroupRelationDao.update(relationEntity, updateWrapper);
            } else {
                //新增对应的关联关系
                attrAttrgroupRelationDao.insert(relationEntity);
            }
        }

    }


    @Override
    public List<AttrEntity> getRelationAttr(Long attrgroupId) {
        //根据分组id查询属性id
        List<AttrAttrgroupRelationEntity> relationEntities = attrAttrgroupRelationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", attrgroupId));

        //属性id
        List<Long> attrIds = relationEntities.stream().map((attr) -> {
            return attr.getAttrId();
        }).collect(Collectors.toList());

        if (attrIds == null || attrIds.size() == 0) {
            return null;
        }
        //查询属性消息
        Collection<AttrEntity> attrEntities = this.listByIds(attrIds);

        return (List<AttrEntity>) attrEntities;
    }

    @Override
    public void deleteRelation(AttrGroupRelationVo[] vos) {

        /**
         * 吧页面提交的属性修改成为关联的中的数据 进行批量删除
         */
        List<AttrAttrgroupRelationEntity> objects = Arrays.asList(vos).stream().map((item) -> {
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            BeanUtils.copyProperties(item, relationEntity);
            return relationEntity;
        }).collect(Collectors.toList());

        attrAttrgroupRelationDao.deleteBatchRelation(objects);

    }

    /**
     * 获取当前分组没有被关联的属性
     *
     * @param params
     * @param attrgroupId
     * @return
     */
    @Override
    public PageUtils getNoRelationAttr(Map<String, Object> params, Long attrgroupId) {
        //1.当前分组只能关联自己所属的分类的所有属性
        AttrGroupEntity groupEntity = attrGroupDao.selectById(attrgroupId);
        Long catelogId = groupEntity.getCatelogId();
        //2.当前分组只能关联别的分组没有关联的属性
        //2.1)找到当前分类下的其他分组
        List<AttrGroupEntity> group =
                attrGroupDao
                        .selectList(new QueryWrapper<AttrGroupEntity>()
                                .eq("catelog_id", catelogId));
        //获取分组id
        List<Long> groupIds = group.stream().map((item) -> {
            return item.getAttrGroupId();
        }).collect(Collectors.toList());

        //2.2)这些分组关联的属性
        List<AttrAttrgroupRelationEntity> groupId =
                attrAttrgroupRelationDao.
                        selectList(new QueryWrapper<AttrAttrgroupRelationEntity>()
                                .in("attr_group_id", groupIds));
        //获取属性id
        List<Long> attrIds = groupId.stream().map((item) -> {
            return item.getAttrId();
        }).collect(Collectors.toList());

        //当前分组可以关联的其他属性
        //2.3)从当前的所有属性中剔除这些属性；
        QueryWrapper<AttrEntity> queryWrapper = new QueryWrapper<AttrEntity>()
                .eq("catelog_id",
                        catelogId).eq("attr_type", ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode());

        if (attrIds != null && attrIds.size() > 0) {
            queryWrapper.notIn("attr_id", attrIds);
        }

        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            queryWrapper.and((w) -> {
                w.eq("attr_id", key).or().like("attr_name", key);
            });
        }
        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), queryWrapper);

        PageUtils pageUtils = new PageUtils(page);

        return pageUtils;
    }

    @Override
    public List<Long> selectSearchAttrIds(List<Long> attrIds) {

        List<Long> ids = this.baseMapper.selectSearchAttrIds(attrIds);

        return ids;
    }


}