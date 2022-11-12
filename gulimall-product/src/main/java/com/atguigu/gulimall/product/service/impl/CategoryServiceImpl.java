package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.vo.Catalog2Vo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import org.springframework.util.StringUtils;


@Service("categoryService")
@Slf4j
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redisson;

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
     *
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
        List<Long> paths = new ArrayList<Long>();
        //最终结果
        List<Long> parentPath = findParentPath(catelogId, paths);
        //逆序排列
        Collections.reverse(parentPath);
        return (Long[]) parentPath.toArray(new Long[parentPath.size()]);
    }

    /**
     * 级联更新所有关联的数据
     *
     * @param category
     */
    @Override
    public void updateByCascade(CategoryEntity category) {
        this.updateById(category);

        //更新关联表
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }

    /**
     * 查询所所有一级分类
     *
     * @return
     */
    @Override
    public List<CategoryEntity> getLavel1Categorys() {
        return this.baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
    }

    /**
     * 查询二级三级分类 引入redisTemplate
     * TODO 产生堆外内存溢出
     * 1、springboot2.0默认使用Lettuce作为操作redis的客服端 使用netty
     * 2、Lettuce的bug导致netty堆外内存移溢出， netty 如果没有指定内容默认使用   Xmx-300m；
     * 3、解决方案:
     * 1、升级netty客服端
     * 2、切换使用jedis客服端
     *
     * @return
     */
    @Override
    public Map<String, List<Catalog2Vo>> getCatelogJson() {

        /**
         * 1、空结果缓存，解决缓存穿透
         * 2、设置过期时间(随机) 解决缓存雪崩
         * 3、加锁  解决缓存击穿
         */
        //1、引入缓存 缓存中所有数据都是JSON
        ValueOperations<String, String> opsForValue = redisTemplate.opsForValue();
        // 获取缓存中的key
        String catelogJson = opsForValue.get("catelogJson");
        if (StringUtils.isEmpty(catelogJson)) {
            System.out.println("缓存不命中...查询数据库...");
            //2、缓存没有数据返回 查询数据库封装到redis中
            Map<String, List<Catalog2Vo>> catelogJsonFromDb = getCatelogJsonFromDbWithRedisLock();
            return catelogJsonFromDb;
        }
        System.out.println("缓存命中直接返回....");
        //4、逆转成需要的数据 指定的对象
        Map<String, List<Catalog2Vo>> result = JSON.parseObject(catelogJson, new TypeReference<Map<String, List<Catalog2Vo>>>() {
        });
        return result;
    }


    /**
     * 从数据库 查询二级分类 并封装分类数据
     * 使用分布式锁
     *
     * @return
     */
    public Map<String, List<Catalog2Vo>> getCatelogJsonFromDbWithRedisLock() {
        //1、占用分布式锁去 redis占坑
        //设置UUID 保证删除锁时当前线程的
        String uuId = UUID.randomUUID().toString();
        //过期时间和占位必须是原子操作
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuId, 300, TimeUnit.SECONDS);
        if (lock) {
            log.info("获取分布锁成功");
            Map<String, List<Catalog2Vo>> dataFromDb;
            try {
                dataFromDb = getDataFromDb();
            } finally {
                //占位成功加锁成功
                //2、设置过期时间 设置30秒
                //对比值+删除锁 = 必须是原子操作 lua脚本解锁
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end ";
                //执行脚本删除释放资源
                Long result = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList("lock"), uuId);
                log.info("释放分布锁成功");
            }
            return dataFromDb;
        } else {
            log.info("获取分布式锁失败..等待重试");
            try {
                //线程暂停 0.3秒 防止线程请求太快 多次失败
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }
            //加锁失败...重试加锁
            //休眠一百ms 重试
            return getCatelogJsonFromDbWithRedisLock();//自旋的方式
        }
    }


    /**
     * 从数据库 查询二级分类 并封装分类数据
     * 使用分布式锁 Redisson
     * 缓存数据一致性问题:
     * 1)、双写模式 更新数据同时 查询 数据缓存数据
     * 2)、失效模式 
     *
     * @return
     */
    public Map<String, List<Catalog2Vo>> getCatelogJsonFromDbWithRedissonLock() {
        //1、获取分布式锁 锁的名字需要一样
        //1.1)细节:锁的的粒度 具体缓存的某个数据 11-号商品 便于处理缓存穿透
        RLock lock = redisson.getLock("catelogJson-lock");
        lock.lock();
        log.info("获取分布锁Redisson成功");
        Map<String, List<Catalog2Vo>> dataFromDb;
        try {
            dataFromDb = getDataFromDb();
        } finally {
            log.info("释放分布锁Redisson成功");
            lock.unlock();
        }
        return dataFromDb;
    }


    /**
     * 从数据库 查询二级分类 并封装分类数据
     * 使用本地锁
     *
     * @return
     */
    public Map<String, List<Catalog2Vo>> getCatelogJsonFromDbWithLocalLock() {

        /**
         *  只要是同一把锁就能锁定这个锁的所有线程
         *   只要是同一吧锁 就能锁住这个线程
         *   TODO: 本地锁 synchronized，JUC(lock) 只能锁住本地资源
         *   TODO: 分布式情况下必须使用分布式锁
         *  加锁方式:
         *   1、synchronized (this) 锁当前示洌 SpringBoot 所有组件在容器中都是单例的。
         *    引发的问题: 分布式的情况下,多个服务就有多个容器会导致产生多个锁
         */

        synchronized (this) {

            /**
             * 解决缓存击穿
             * 加锁逻辑:
             *   得到锁之后才去缓存中确定一次 如果没有才需要继续查询
             */
            return getDataFromDb();
        }
    }

    /**
     * 从数据获取数据
     *
     * @return
     */
    private Map<String, List<Catalog2Vo>> getDataFromDb() {
        String catelogJson = redisTemplate.opsForValue().get("catelogJson");
        if (!StringUtils.isEmpty(catelogJson)) {
            //4、逆转成需要的数据 指定的对象
            Map<String, List<Catalog2Vo>> result = JSON.parseObject(catelogJson, new TypeReference<Map<String, List<Catalog2Vo>>>() {
            });
            //返回缓存中获取的数据
            return result;
        }
        System.out.println("查询了数据库");
        //查出所有数据
        List<CategoryEntity> entities = this.baseMapper.selectList(null);
        //全部一级分类
        List<CategoryEntity> categorys = getParent_cid(entities, 0L);
        //要返回的数据
        Map<String, List<Catalog2Vo>> collect =
                categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
                    //1、遍历每一个一级分类
                    //查到二级分类
                    List<CategoryEntity> categoryEntities = getParent_cid(entities, v.getCatId());
                    List<Catalog2Vo> catalog2Vos = null;
                    if (categoryEntities != null) {
                        //当前分类二级分类
                        catalog2Vos = categoryEntities.stream().map(l2 -> {
                            Catalog2Vo catalog2Vo = new Catalog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                            //找出当前二级分类的三级分类
                            List<CategoryEntity> entityList3 = getParent_cid(entities, l2.getCatId());
                            List<Catalog2Vo.Catelog3Vo> catelog3Vos = entityList3.stream().map(l3 -> {
                                Catalog2Vo.Catelog3Vo catelog3Vo = new Catalog2Vo.Catelog3Vo();
                                catelog3Vo.setCatalog2Id(l2.getCatId().toString());
                                catelog3Vo.setName(l3.getName());
                                catelog3Vo.setId(l3.getCatId().toString());
                                return catelog3Vo;
                            }).collect(Collectors.toList());
                            catalog2Vo.setCatalog3List(catelog3Vos);
                            return catalog2Vo;
                        }).collect(Collectors.toList());
                    }
                    return catalog2Vos;
                }));
        //3、查到的数据获取缓存、讲对象转出JSON
        String toJSONString = JSON.toJSONString(collect);
        //设置一天的过期时间
        redisTemplate.opsForValue().set("catelogJson", toJSONString, 1, TimeUnit.DAYS);
        return collect;
    }


    private List<CategoryEntity> getParent_cid(List<CategoryEntity> entities, Long parent_cid) {

        List<CategoryEntity> collect = entities.stream().filter(item -> item.getParentCid() == parent_cid).collect(Collectors.toList());

        return collect;

    }


    //225,25,2
    private List<Long> findParentPath(Long catelogId, List<Long> paths) {
        //找到父级id收集
        //1.收集当前节点id
        paths.add(catelogId);
        //根据当前id查询
        CategoryEntity byId = this.getById(catelogId);
        if (byId.getParentCid() != 0) {
            findParentPath(byId.getParentCid(), paths);
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