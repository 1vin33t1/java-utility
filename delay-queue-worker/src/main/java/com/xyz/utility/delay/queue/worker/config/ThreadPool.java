package com.xyz.utility.delay.queue.worker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ThreadPool {

    public static final String ASYNC_THREAD_POOL = "asyncThreadPool";

    @Primary
    @Bean(name = ASYNC_THREAD_POOL)
    public AsyncTaskExecutor asyncThreadPool(
            @Value("${thread.async.core-pool-size}") int corePoolSize,
            @Value("${thread.async.max-pool-size}") int maxPoolSize,
            @Value("${thread.async.queue-size}") int queueCapacity
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("Async-Thread-Pool-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }


}
