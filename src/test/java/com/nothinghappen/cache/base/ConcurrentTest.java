package com.nothinghappen.cache.base;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ConcurrentTest {

    private static ExecutorService executor = Executors.newCachedThreadPool();

    public static void run(int threadNums, Runnable run) {
        Future[] futures = runAsync(threadNums, run);
        // wait
        for (int i = 0; i < threadNums; i++) {
            try {
                futures[i].get();
            } catch (Exception ex){}
        }
    }

    public static Future[] runAsync(int threadNums, Runnable run) {
        CyclicBarrier cb = new CyclicBarrier(threadNums);
        Future[] futures = new Future[threadNums];
        for (int i = 0; i < threadNums; i++) {
            futures[i] = executor.submit(() -> {
                try {
                    cb.await();
                } catch (Exception e) {
                }
                run.run();
            });
        }
        return futures;
    }

    public static <T> Future<T>[] supplyAsync(int threadNums, Callable<T> callable) {
        CyclicBarrier cb = new CyclicBarrier(threadNums);
        Future<T>[] futures = new Future[threadNums];
        for (int i = 0; i < threadNums; i++) {
            futures[i] = executor.submit(() -> {
                try {
                    cb.await();
                } catch (Exception e) {
                }
                return callable.call();
            });
        }
        return futures;
    }
}
