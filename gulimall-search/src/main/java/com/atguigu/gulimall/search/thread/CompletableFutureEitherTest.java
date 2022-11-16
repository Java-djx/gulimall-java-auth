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
 *  组合之前一个完成
 */
public class CompletableFutureEitherTest {

    public static ExecutorService executor = Executors.newFixedThreadPool(10);


    public static void main(String[] args) throws ExecutionException, InterruptedException {

        System.out.println("main....start...");

        CompletableFuture<Object> future1 = CompletableFuture.supplyAsync(() -> {
            System.out.println("future1开始" + Thread.currentThread().getId());
            Integer i = 10 / 2;
            System.out.println("future1结束:" + i);
            return i;
        }, executor);

        CompletableFuture<Object> future2 = CompletableFuture.supplyAsync(() -> {
            System.out.println("future2开始" + Thread.currentThread().getId());
            try {
                Thread.sleep(200);
                System.out.println("future2结束:");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "hello";
        }, executor);

        /**
         * 两个任务只要有一个任务完成,当前就执行 无返回值
         * 无法感受异步编排的结果
         */
//        future1.runAfterEitherAsync(future2,()->{
//            System.out.println("任务3开始....");
//        },executor);

        /**
         * 两个任务只要有一个任务完成,当前就执行 无返回值
         * 可以感受异步编排的结果
         */
//        CompletableFuture<Void> future = future1.acceptEitherAsync(future2, (res) -> {
//            System.out.println("任务3开始...." + res);
//        }, executor);

        CompletableFuture<String> future = future1.applyToEitherAsync(future2, (res) -> {
            System.out.println("任务3开始...." + res);
            return res.toString() + "---world";
        }, executor);

        System.out.printf("结果", future.get());


        System.out.println("main....end...");


    }

}
