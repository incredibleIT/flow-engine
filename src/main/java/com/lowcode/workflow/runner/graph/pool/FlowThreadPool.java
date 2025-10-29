package com.lowcode.workflow.runner.graph.pool;

import com.lowcode.workflow.runner.graph.exception.custom.CustomException;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class FlowThreadPool {

    private final ThreadPoolExecutor threadPoolExecutor;

    // 定义线程池参数
    public FlowThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTimeSeconds, int queueCapacity, String threadNamePrefix, RejectedExecutionHandler rejectedExecutionHandler) {

        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(queueCapacity);

        ThreadFactory threadFactory = new ThreadFactory() {

            final AtomicLong atomicLong = new AtomicLong(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, threadNamePrefix + "-" + atomicLong.getAndIncrement());
                thread.setDaemon(false); // 非守护线程，确保 JVM 不会提前退出
                thread.setUncaughtExceptionHandler((t, ex) -> {
                    System.err.println("线程 [" + t.getName() + "] 发生未捕获异常: " + ex.getMessage());
                    log.error("线程 [" + t.getName() + "] 发生未捕获异常: " + ex.getMessage(), ex);
                    throw new CustomException(500, "线程 [" + t.getName() + "] 发生未捕获异常: " + ex.getMessage());
                });
                return thread;
            }
        };

        this.threadPoolExecutor = new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTimeSeconds,
                TimeUnit.SECONDS,
                workQueue,
                threadFactory,
                rejectedExecutionHandler != null ? rejectedExecutionHandler : new ThreadPoolExecutor.AbortPolicy()
        );
    }

    // 无返回值任务
    public void execute(Runnable runnable) {
        this.threadPoolExecutor.execute(runnable);
    }

    public ThreadPoolExecutor getThreadPoolExecutor() {
        return this.threadPoolExecutor;
    }

    // 关闭线程池
    public void shutdown() {
        this.threadPoolExecutor.shutdown();

        try {
            if (!this.threadPoolExecutor.awaitTermination(100, TimeUnit.SECONDS)) {
                log.warn("线程池 {} 未在 100 秒内终止, 正在强制关闭", this.threadPoolExecutor);
                this.threadPoolExecutor.shutdownNow();
                if (!this.threadPoolExecutor.awaitTermination(100, TimeUnit.SECONDS)) {
                    throw new CustomException(500, "线程池 " + this.threadPoolExecutor + " 强制关闭后仍未终止");
                }
            }

        } catch (InterruptedException e) {
            throw new CustomException(500, "线程池 " + this.threadPoolExecutor + " 强制关闭时被中断");
        }
    }

    // 立即关闭
    public void shutdownNow() {
        this.threadPoolExecutor.shutdownNow();
    }




}
