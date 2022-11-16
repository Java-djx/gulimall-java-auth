package com.atguigu.gulimall.search.thread;

import org.elasticsearch.client.ml.EvaluateDataFrameRequest;

import java.util.concurrent.*;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/15 15:30
 */
public class ThreadTest {

    //当前系统中只有一两个线程池，每个异步任务，提交给线程吃让他自己去执行
    public static ExecutorService service = Executors.newFixedThreadPool(10);


    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("main....start...");
        /**
         * 1）、继承 Thread
         *     new Thread01().start();
         * 2）、实现 Runnable 接口
         *     Runable01 runable01 = new Runable01();
         *     new Thread(runable01).start();
         * 3）、实现 Callable 接口 + FutureTask （可以拿到返回结果，可以处理异常）
         *         FutureTask<Integer> futureTask = new FutureTask<>(new Callable01());
         *         new Thread(futureTask).start();
         *         //阻塞等待 拿到最终的返回结果
         *         System.out.println(futureTask.get());
         * 4）、线程池【ExecutorService】
         *      1、给线程池提交任务
         *       service.execute(new Runable01());
         *      2、创建:
         *       1.
         *
         * 总结区别:
         *   1.2 不能得到返回值 3.可以得到返回值
         *   1.2.3 不能控制资源
         *   线程池.可以控制资源,性能稳定。
         *
         */
        //在业务代码中  但凡三种启动方式不用、应该吧所有的异步任务交给线程池来执行
//        new Thread(() -> System.out.println("hello")).start();

        /**
         * int corePoolSize, 核心线程数【一直存在除非allowCoreThreadTimeOut】 线程池创建好就准备就绪
         * int maximumPoolSize  最大线程数量,控制资源
         * long keepAliveTime, 存活时间,释放空闲的线程 不会释放核心线程
         * TimeUnit unit, 存活时间单位
         * BlockingQueue<Runnable> workQueue, 阻塞队列 如果任务有很多,就会讲目前多的任务放在队列中，当线程空闲之后在执行
         * ThreadFactory threadFactory 创建的工厂
         * RejectedExecutionHandler handler：如果队列满了按照我们指定的策略拒绝执行任务
         *
         运行流程：
         1、线程池创建，准备好 core 数量的核心线程，准备接受任务
         2、新的任务进来，用 core 准备好的空闲线程执行。
         (1) 、core 满了，就将再进来的任务放入阻塞队列中。空闲的 core 就会自己去阻塞队
         列获取任务执行
         (2) 、阻塞队列满了，就直接开新线程执行，最大只能开到 max 指定的数量
         (3) 、max 都执行好了。Max-core 数量空闲的线程会在 keepAliveTime 指定的时间后自
         动销毁。最终保持到 core 大小
         (4) 、如果线程数开到了 max 的数量，还有新任务进来，就会使用 reject 指定的拒绝策
         略进行处理
         3、所有的线程创建都是由指定的 factory 创建的。
         */

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                5, 200, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>(10000),
                Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());


//        Executors.newCachedThreadPool();coro:0 所有都可回收
//        Executors.newFixedThreadPool();固定大小
//        Executors.newScheduledThreadPool(); 定时任务
//        Executors.newSingleThreadExecutor(); 单线程 获取任务单个执行


        System.out.println("main...end....");
    }


    public static class Thread01 extends Thread {
        @Override
        public void run() {
            System.out.println("当前线程" + Thread.currentThread().getId());
            Integer i = 10 / 2;
            System.out.println("运行结果:" + i);
        }
    }

    public static class Runable01 implements Runnable {

        @Override
        public void run() {
            System.out.println("当前线程" + Thread.currentThread().getId());
            Integer i = 10 / 2;
            System.out.println("运行结果:" + i);
        }
    }

    public static class Callable01 implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            System.out.println("当前线程" + Thread.currentThread().getId());
            Integer i = 10 / 2;
            System.out.println("运行结果:" + i);
            return i;
        }
    }
}
