package com.xyz.utility.delay.queue.worker.annotation;

import java.lang.annotation.*;

@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DuplicateMessageDetector {
}
