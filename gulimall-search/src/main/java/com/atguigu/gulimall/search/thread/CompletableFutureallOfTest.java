package com.atguigu.gulimall.search.thread;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/15 17:38
 * 异步编排
 * 异步编排的创建
 * 有返回值和无返回值
 * 多任务组合执行
 */
public class CompletableFutureallOfTest {

    public static ExecutorService executor = Executors.newFixedThreadPool(10);


    public static void main(String[] args) throws ExecutionException, InterruptedException {

        System.out.println("main....start...");
        CompletableFuture<Object> futureImg = CompletableFuture.supplyAsync(() -> {
            System.out.println("查询商品的图片消息");
            return "futureImg.png";
        }, executor);
        CompletableFuture<Object> futureAttr = CompletableFuture.supplyAsync(() -> {
            System.out.println("查询商品的属性");
            return "黑色+256G";
        }, executor);
        CompletableFuture<Object> futureDesc = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(3000);
                System.out.println("查询商品的介绍");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "华为Pro50s";
        }, executor);

        CompletableFuture<Object> allOf = CompletableFuture.anyOf(futureImg, futureAttr, futureDesc);

        System.out.println("allOf.get() = " + allOf.get());


        System.out.println("main....end...");
    }

}
