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
 *  组合之前两个都完成
 */
public class CompletableFutureBothTest {

    public static ExecutorService executor = Executors.newFixedThreadPool(10);


    public static void main(String[] args) throws ExecutionException, InterruptedException {

        System.out.println("main....start...");

        CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> {
            System.out.println("future1开始" + Thread.currentThread().getId());
            Integer i = 10 / 2;
            System.out.println("future1结束:" + i);
            return i;
        }, executor);

        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            System.out.println("future2开始" + Thread.currentThread().getId());
            System.out.println("future2结束:");
            return "hello";
        }, executor);

//        future1.runAfterBothAsync(future2,()->{
//            System.out.println("任务3开始....");
//        },executor);

//        future1.thenAcceptBothAsync(future2,(f1, f2) -> {
//            System.out.println("任务3开始....之前的结果:"+f1+"//"+f2);
//        },executor);

        CompletableFuture<String> async = future1.thenCombineAsync(future2, (f1, f2) -> {
            System.out.println("任务3开始....之前的结果:" + f1 + "//" + f2);
            return f1 + "---->" + f2 + "world";
        }, executor);
        System.out.println("async.get() = " + async.get());


        System.out.println("main....end...");


    }

}
