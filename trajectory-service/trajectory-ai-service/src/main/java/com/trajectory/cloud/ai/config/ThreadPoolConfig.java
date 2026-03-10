package com.trajectory.cloud.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池配置
 *
 * @author StephenQiu30
 */
@Slf4j
@Configuration
public class ThreadPoolConfig {

    /**
     * 自定义线程池，用于收敛 BI 分析等耗时任务
     * 核心参数根据系统 CPU 核心数动态调整
     *
     * @return 线程池执行器
     */
    @Bean(name = "biThreadPoolExecutor")
    public ThreadPoolExecutor biThreadPoolExecutor() {
        // 核心线程数：CPU 核心数的 2 倍
        int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;
        // 最大线程数：核心线程数的 2 倍
        int maxPoolSize = corePoolSize * 2;
        // 非核心线程空闲存活时间
        long keepAliveTime = 60L;
        // 阻塞队列容量
        int queueCapacity = 500;

        return new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                new ThreadFactory() {
                    private final AtomicInteger count = new AtomicInteger(1);

                    @Override
                    public Thread newThread(@NotNull Runnable r) {
                        Thread thread = new Thread(r);
                        thread.setName("bi-analysis-thread-" + count.getAndIncrement());
                        return thread;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
