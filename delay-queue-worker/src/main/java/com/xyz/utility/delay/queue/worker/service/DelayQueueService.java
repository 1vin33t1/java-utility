package com.xyz.utility.delay.queue.worker.service;

import com.xyz.utility.common.constant.UtilityConstant;
import com.xyz.utility.common.kafka.ProducerTemplate;
import com.xyz.utility.common.util.CommonUtility;
import com.xyz.utility.delay.queue.worker.annotation.DuplicateMessageDetector;
import com.xyz.utility.delay.queue.worker.annotation.JaegerKafkaTracing;
import com.xyz.utility.delay.queue.worker.config.RuntimeProperties;
import com.xyz.utility.persistence.model.KafkaMessageCache;
import com.xyz.utility.persistence.repository.aerospike.KafkaMessageIdAndCacheRepository;
import io.opentracing.contrib.kafka.TracingKafkaUtils;
import io.opentracing.util.GlobalTracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class DelayQueueService {

    private final RuntimeProperties runtimeProperties;
    private final AsyncTaskExecutor asyncTaskExecutor;
    private final ProducerTemplate producerTemplate;
    private final KafkaMessageIdAndCacheRepository kafkaMessageIdAndCacheRepository;
    private final DelayFailureHandler failureHandler;


    @DuplicateMessageDetector
    @JaegerKafkaTracing(operationName = "delay-pre-processor")
    public void preProcess(String messageId, MessageHeaders headers, KafkaMessageCache messageCache) {
        log.info("message received for delay request : {}", messageCache);
        if (!this.readyForProcess(messageCache))
            return;
        if (!this.saveOrFailMessage(messageCache)) {
            log.info("failed to save message sending for retry, {}", messageCache);
            producerTemplate.send(new ProducerRecord<>(runtimeProperties.getDelayKafkaTopic(), null, System.currentTimeMillis(),
                    messageCache.getKey(), messageCache, null));
        }
    }

    @DuplicateMessageDetector
    @JaegerKafkaTracing(operationName = "delay-post-processor")
    public void postProcess(String messageId, MessageHeaders headers, KafkaMessageCache messageCache) {
        try {
            this.sendMessage(messageCache);
        } finally {
            asyncTaskExecutor.execute(() -> deleteCache(messageCache));
        }
    }

    private boolean readyForProcess(KafkaMessageCache request) {
        if (System.currentTimeMillis() + runtimeProperties.getToleranceTime() >= request.getPostTime()) {
            log.info("no delay required so posting the message right now {}", request);
            this.sendMessage(request);
            return false;
        }
        if (!StringUtils.hasText(request.getId())) {
            request.setId(CommonUtility.generateId(request.getTopic(), request.getKey()));
        }
        request.setTries(request.getTries() + 1);
        return true;
    }

    protected boolean saveOrFailMessage(KafkaMessageCache messageCache) {
        long delay = messageCache.getPostTime() - System.currentTimeMillis();
        try {
            log.info("saving message {} to aerospike with delay {} ms", messageCache, delay);
            injectFutureTrace(messageCache);
            kafkaMessageIdAndCacheRepository.save(messageCache, (int) (delay / 1000));
            return true;
        } catch (Exception ex) {
            log.error("exception occurred while saving the message to the aerospike", ex);
            if (messageCache.getTries() >= runtimeProperties.getTriesMaxCount()) {
                failureHandler.notifyClient(messageCache, UtilityConstant.Kafka.AEROSPIKE_SAVE_RETRY_EXHAUST, ex);
                return true;
            }
        }
        return false;
    }

    private void injectFutureTrace(KafkaMessageCache messageCache) {
        if (messageCache.getHeaders() == null)
            messageCache.setHeaders(new HashMap<>());
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(runtimeProperties.getPostDelayKafkaTopicName(), "");
        TracingKafkaUtils.buildAndInjectSpan(producerRecord, GlobalTracer.get()).finish();
        producerRecord.headers().forEach(header -> messageCache.getHeaders().put(header.key(), new String(header.value())));
    }

    protected void sendMessage(KafkaMessageCache messageCache) {
        Headers headers = new RecordHeaders();
        if (messageCache.getFailurePostCallbackUrl() != null)
            headers.add(UtilityConstant.Kafka.FAILURE_CALLBACK_URL, messageCache.getFailurePostCallbackUrl().getBytes(StandardCharsets.UTF_8));
        if (!CollectionUtils.isEmpty(messageCache.getHeaders())) {
            messageCache.getHeaders().forEach((key, value) -> headers.add(key, value != null ? value.getBytes(StandardCharsets.UTF_8) : null));
        }
        producerTemplate.send(new ProducerRecord<>(messageCache.getTopic(), null, System.currentTimeMillis(),
                messageCache.getKey(), messageCache.getValue(), headers));
    }

    public void deleteCache(KafkaMessageCache messageCache) {
        log.info("deleting the message from cache, {}", messageCache.getId());
        kafkaMessageIdAndCacheRepository.deleteById(messageCache.getId());
    }

}
