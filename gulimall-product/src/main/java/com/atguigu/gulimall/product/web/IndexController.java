package com.atguigu.gulimall.product.web;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catalog2Vo;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/9 21:10
 */
@Controller
public class IndexController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedissonClient redisson;

    @Autowired
    private StringRedisTemplate redisTemplate;


    /**
     * 跳转到首页查询所有 一级分类
     *
     * @return
     */
    @GetMapping({"/", "/index.html"})
    public String indexPage(Model model) {

        List<CategoryEntity> categoryEntities = categoryService.getLavel1Categorys();

        model.addAttribute("categorys", categoryEntities);

        return "index";
    }

    @GetMapping("/index/json/catalog")
    @ResponseBody
    public Map<String, List<Catalog2Vo>> getCatelogJson() {

        Map<String, List<Catalog2Vo>> categoryEntities = categoryService.getCatelogJson();

        return categoryEntities;
    }


    @GetMapping("/hello")
    @ResponseBody
    public String HelloHandler() {
        //1、获取一把锁还要锁的名字一样就是同一吧锁
        RLock lock = redisson.getLock("andLock");

//        加锁 阻塞式等待获取锁,默认加锁的时长是30秒
//          锁的自延迟，如果业务超级长 Redisson自动给锁需时间
//          30秒自动关机,如果业务处理完毕就会删除锁
        lock.lock();
//        问题:lock.lock(10, TimeUnit.SECONDS);在锁到了之后不会自动续期
//        1、如果我们传递了锁的过期时间就这行lun脚本 执行占锁
//        2.如果没指定时间就是用默认的30秒 看门狗 默认时间
//          只要占锁成功就会启动一个定时任务【重新给锁设置过期时间，新的过期时间就是看门狗的过期时间】 每隔时间10s自动续机 续成满时间

        /**
         * 最佳实战
         *  1、 lock.lock(10, TimeUnit.SECONDS);//10秒自动过期,自动解锁时间一定大于业务处理时长
         *  省掉了整个续期时间。手动解锁
         */

        try {
            System.out.println("加锁成功执行业务代码" + Thread.currentThread().getId());
//            模拟业务等待
            Thread.sleep(3000);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
//            释放锁
            lock.unlock();
            System.out.println("释放锁成功！" + Thread.currentThread().getId());
        }

        return "hello";
    }


    /**
     * Redisson 写锁
     * 1、保证一定能读到最新的数据 修改期间 写锁是一个互斥的锁 读锁是一个共享锁
     * 2、写锁没释放读锁就必须一直等待
     * 3、写+读 等待写锁释放
     * 4、写+写 阻塞获取锁
     * 5、读+写 有读缩的时候写锁也必须等待
     * 6、读+读 相当于无锁 并发读 相当于无锁 只会在redis中记录好 当前的锁好
     *
     * @return
     */
    @GetMapping("/write")
    @ResponseBody
    public String writeValue() {
        RReadWriteLock readWriteLock = redisson.getReadWriteLock("rw-lock");
        RLock lock = readWriteLock.writeLock();
        String uuid = "";
        try {
            //加写锁
            lock.lock();
            System.out.println("写锁加锁成功" + Thread.currentThread().getId());
            uuid = UUID.randomUUID().toString();
            Thread.sleep(30000);
            redisTemplate.opsForValue().set("writeValue", uuid);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            //释放锁
            lock.unlock();
            System.out.println("写锁释放成功" + Thread.currentThread().getId());
        }

        return uuid;
    }

    /**
     * 读取数据 加 读锁
     *
     * @return
     */
    @GetMapping("/read")
    @ResponseBody
    public String readValue() {
        RReadWriteLock readWriteLock = redisson.getReadWriteLock("rw-lock");
        //加上读锁
        RLock lock = readWriteLock.readLock();
        lock.lock();
        String uuid = "";
        try {
            System.out.println("读锁加锁成功" + Thread.currentThread().getId());
            uuid = redisTemplate.opsForValue().get("writeValue");
            Thread.sleep(30000);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //释放锁
            lock.unlock();
            System.out.println("读锁加锁成功" + Thread.currentThread().getId());

        }
        return uuid;
    }


    /**
     * 闭锁
     * 模拟门卫等到锁门 等待全部的班级走了才释放锁
     *
     * @return
     */
    @GetMapping("/lockDoor")
    @ResponseBody
    public String lockDoor() throws InterruptedException {
        RCountDownLatch door = redisson.getCountDownLatch("door");
        door.trySetCount(5);//设置释放锁
        door.await();//等待锁门
        return "放假啦...关门...释放锁";
    }

    @GetMapping("/gogogo/{id}")
    @ResponseBody
    public String gogogo(@PathVariable Long id) {

        //获取同一把闭锁
        RCountDownLatch door = redisson.getCountDownLatch("door");
        door.countDown();//计数减1

        return id + "班的人都走啦";
    }


    /**
     * 模拟场景车库停车:
     * 1、有限的车库只能让有限的车停车
     *
     * @return
     */
    @GetMapping("/park")
    @ResponseBody
    public String park() throws InterruptedException {

//        获取信号量
        RSemaphore park = redisson.getSemaphore("park");
//        阻塞的获取一个值 占一个车位
        park.acquire();
        return "acquire";
    }

    @GetMapping("/go")
    @ResponseBody
    public String go() throws InterruptedException {

//        获取信号量
        RSemaphore park = redisson.getSemaphore("park");
//        释放一个信号 释放一个车位
        park.release();
        return "release";
    }
}
