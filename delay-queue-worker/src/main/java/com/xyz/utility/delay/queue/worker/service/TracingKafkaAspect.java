package com.xyz.utility.delay.queue.worker.service;

import com.xyz.utility.delay.queue.worker.annotation.JaegerKafkaTracing;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.contrib.kafka.TracingKafkaUtils;
import io.opentracing.util.GlobalTracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.kafka.support.DefaultKafkaHeaderMapper;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;

@Aspect
@Slf4j
@Service
@RequiredArgsConstructor
public class TracingKafkaAspect {

    @Around(value = "@annotation(com.xyz.utility.delay.queue.worker.annotation.JaegerKafkaTracing) && @annotation(jaegerKafkaTracing) && args(id,headers,..)",
            argNames = "joinPoint,jaegerKafkaTracing,id,headers")
    public Object around(ProceedingJoinPoint joinPoint, final JaegerKafkaTracing jaegerKafkaTracing, String id, MessageHeaders headers) throws Throwable {
        Tracer tracer = GlobalTracer.get();
        RecordHeaders recordHeaders = new RecordHeaders();
        DefaultKafkaHeaderMapper mapper = new DefaultKafkaHeaderMapper();
        mapper.fromHeaders(headers, recordHeaders);
        SpanContext spanContext = TracingKafkaUtils.extractSpanContext(recordHeaders, tracer);
        Span span = tracer.buildSpan(jaegerKafkaTracing.operationName())
                .asChildOf(spanContext)
                .start();
        try (Scope ignored = tracer.activateSpan(span)) {
            return joinPoint.proceed();
        } finally {
            span.finish();
        }
    }

}
