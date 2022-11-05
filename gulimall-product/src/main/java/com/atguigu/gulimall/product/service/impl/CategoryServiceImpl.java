package com.atguigu.gulimall.product.service.impl;

import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //1.查询所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);

        //2.组装父子的树形结构
        // 2.1先找出所有一级分类
        List<CategoryEntity> lave1Menus = entities.stream().
                filter(categoryEntity -> categoryEntity.getParentCid() == 0)
                .map((menu) -> {
                    //找出子菜单 当前菜单 和 全部的菜单
                    menu.setChildren(getChildren(menu, entities));
                    return menu;
                }).sorted((menu1, menu2) ->
                        (menu1.getSort() == null ? 0 : menu1.getSort()) -
                                (menu2.getSort() == null ? 0 : menu2.getSort()))
                .collect(Collectors.toList());

        return lave1Menus;
    }

    /**
     * 自定义删除方法
     * @param asList
     */
    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO 1.检查当前删除的菜单 是否被引用
        //采用逻辑删除替换物理删除
        baseMapper.deleteBatchIds(asList);
    }

    //[2,25,225]
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        //作为容器
        List<Long> paths=new ArrayList<Long>();
        //最终结果
        List<Long> parentPath = findParentPath(catelogId, paths);
        //逆序排列
        Collections.reverse(parentPath);
        return (Long[]) parentPath.toArray(new Long[parentPath.size()]);
    }

    /**
     * 级联更新所有关联的数据
     * @param category
     */
    @Override
    public void updateByCascade(CategoryEntity category) {
        this.updateById(category);

        //更新关联表
        categoryBrandRelationService.updateCategory(category.getCatId(),category.getName());
    }

    //225,25,2
    private List<Long> findParentPath(Long catelogId,List<Long> paths){
        //找到父级id收集
        //1.收集当前节点id
        paths.add(catelogId);
        //根据当前id查询
        CategoryEntity byId = this.getById(catelogId);
        if (byId.getParentCid()!=0){
            findParentPath(byId.getParentCid(),paths);
        }
        return paths;
    }


    /**
     * 递归寻找当前菜单的子菜单
     *
     * @param root 当前菜单
     * @param all  全部菜单
     * @return
     */
    private List<CategoryEntity> getChildren(CategoryEntity root, List<CategoryEntity> all) {

        //首先获取当前菜单 和  全部的菜单
        //过滤全部的菜单 找到 菜单以及子菜单
        List<CategoryEntity> children = all.stream()
                //过滤当前集合 比较 如果当前全部中的菜单中的pid 等于上一级菜单的 id
                .filter(categoryEntity ->
                categoryEntity.getParentCid() == root.getCatId()
        ).map((categoryEntity) -> {
            //找到子菜单
            categoryEntity.setChildren(getChildren(categoryEntity, all));
            return categoryEntity;
        }).sorted((menu1, menu2) -> (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort())
        ).collect(Collectors.toList());

        return children;
    }

}