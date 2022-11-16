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
 */
public class CompletableFutureTest {

    public static ExecutorService executor = Executors.newFixedThreadPool(10);


    public static void main(String[] args) throws ExecutionException, InterruptedException {

        System.out.println("main....start...");

        /*
         普通处理
         */
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            System.out.println("当前线程" + Thread.currentThread().getId());
            Integer i = 10 / 2;
            System.out.println("运行结果:" + i);
        }, executor);

        /**
         * 异步结果之后的感知
         */
        CompletableFuture<Integer> supplyAsync = CompletableFuture.supplyAsync(() -> {
            System.out.println("当前线程" + Thread.currentThread().getId());
            Integer i = 10 / 0;
            System.out.println("运行结果:" + i);
            return i;
        }, executor).whenCompleteAsync((res, excption) -> {
            System.out.println("异步任务执行完成，结果是:" + res + "异常是:" + excption);
        }).exceptionally(throwable -> {
            return 0;
        });

        CompletableFuture<Integer> supplyAsync2 = CompletableFuture.supplyAsync(() -> {
            System.out.println("当前线程" + Thread.currentThread().getId());
            Integer i = 10 / 0;
            System.out.println("运行结果:" + i);
            return i;
        }, executor).handle((res, thr) -> {
            if (res != null) return res * 2;
            if (thr != null) return 1;
            return 0;
        });

        /**
         * 异步编排串行执行
         * .thenRunAsync(() -> {
         *             System.out.println("任务2执行完成！");
         *         }, executor)
         *         不能感知上一步的执行结果
         * .thenAcceptAsync(res -> {
         *             System.out.println("任务2启动了" + res);
         *         }, executor);
         *         可以感知到上一步执行结果
         *
         *
         //         */
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            System.out.println("当前线程" + Thread.currentThread().getId());
            Integer i = 10 / 2;
            System.out.println("运行结果:" + i);
            return i;
        }, executor).thenApplyAsync((res) -> {
            System.out.println("任务2启动了" + res);
            return res + "hello";
        }, executor);


        System.out.println("main....end...");


    }

}
