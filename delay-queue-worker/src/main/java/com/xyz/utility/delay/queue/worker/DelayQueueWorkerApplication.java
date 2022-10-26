package com.xyz.utility.delay.queue.worker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Slf4j
@EnableAspectJAutoProxy
@SpringBootApplication(scanBasePackages = "com.xyz.utility")
public class DelayQueueWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DelayQueueWorkerApplication.class, args);
        log.debug("delay queue worker started");
    }

}