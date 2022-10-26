package com.xyz.utility.delay.queue.worker.listener;

import com.xyz.utility.common.constant.UtilityConstant;
import com.xyz.utility.common.kafka.ProducerTemplate;
import com.xyz.utility.common.util.JsonConversionUtility;
import com.xyz.utility.delay.queue.worker.config.Consumer;
import com.xyz.utility.delay.queue.worker.dto.AerospikeEventValue;
import com.xyz.utility.delay.queue.worker.service.DelayQueueService;
import com.xyz.utility.persistence.model.KafkaMessageCache;
import com.xyz.utility.persistence.repository.aerospike.KafkaMessageCacheRepository;
import com.xyz.utility.persistence.repository.aerospike.KafkaProcessedIdRepository;
import io.opentracing.contrib.kafka.TracingKafkaUtils;
import io.opentracing.util.GlobalTracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class DelayQueueProcessor {

    private final DelayQueueService delayQueueService;
    private final KafkaMessageCacheRepository kafkaMessageCacheRepository;
    private final KafkaProcessedIdRepository kafkaProcessedIdRepository;
    private final ProducerTemplate producerTemplate;

    @KafkaListener(id = "delay-pre-processor", idIsGroup = false, topics = "${kafka.pre.delay.topic.name}",
            concurrency = "${kafka.pre.delay.topic.concurrency}", containerFactory = Consumer.JAEGER_KAFKA_CONTAINER)
    public void delayPreProcessor(@Header(value = UtilityConstant.Kafka.MESSAGE_ID, required = false) String kfid,
                                  @org.springframework.messaging.handler.annotation.Headers MessageHeaders headers,
                                  @Payload KafkaMessageCache messageCache) {
        delayQueueService.preProcess(kfid, headers, messageCache);
    }

    @KafkaListener(id = "delay-post-processor", idIsGroup = false, topics = "${kafka.post.delay.topic.name}",
            concurrency = "${kafka.post.delay.topic.concurrency}", containerFactory = Consumer.DEFAULT_KAFKA_CONTAINER)
    public void delayPostProcessor(ConsumerRecord<String, String> record) {
        log.info("message received by delay-post-processor value: {} key: {}", record.value(), record.key());
        AerospikeEventValue value = JsonConversionUtility.classFromJsonJavaReflectionSilently(record.value(), AerospikeEventValue.class);
        if (value != null && value.getMetadata() != null && UtilityConstant.DELETE_EVENT.equals(value.getMetadata().getMsg())) {
            String idKey = value.getMetadata().getUserKey();
            if (idKey != null) {
                kafkaMessageCacheRepository.findById(idKey).ifPresent(kafkaMessageCache -> {
                    if (isDuplicate(kafkaMessageCache)) {
                        delayQueueService.deleteCache(kafkaMessageCache);
                        return;
                    }
                    MessageHeaders messageHeaders = injectTracingDetails(record, kafkaMessageCache);
                    delayQueueService.postProcess(kafkaMessageCache.getId(), messageHeaders, kafkaMessageCache);
                });
            }
        }
    }

    private boolean isDuplicate(KafkaMessageCache messageCache) {
        try {
            if (kafkaProcessedIdRepository.existsById(messageCache.getId())) {
                log.warn("post processing detected a duplicate event : {}", messageCache.getId());
                return true;
            }
        } catch (Exception ex) {
            log.error("Error while checking existence of processed-id {} in aerospike", messageCache.getId(), ex);
        }
        return false;
    }

    private MessageHeaders injectTracingDetails(ConsumerRecord<String, String> record, KafkaMessageCache kafkaMessageCache) {
        Map<String, Object> messageHeaders = new HashMap<>();
        if (!CollectionUtils.isEmpty(kafkaMessageCache.getHeaders())) {
            Headers headers = new RecordHeaders();
            kafkaMessageCache.getHeaders().forEach((k, v) -> {
                if (v != null) {
                    headers.add(k, v.getBytes(StandardCharsets.UTF_8));
                    messageHeaders.put(k, v);
                }
            });
            var duplicateRecord = new ConsumerRecord<>(record.topic(), record.partition(), record.offset(),
                    record.timestamp(), record.timestampType(), -1L, record.serializedKeySize(),
                    record.serializedValueSize(), record.key(), record.value(), headers);
            TracingKafkaUtils.buildAndFinishChildSpan(duplicateRecord, GlobalTracer.get());
        }
        return new MessageHeaders(messageHeaders);
    }


    AtomicLong totalTimeDiff = new AtomicLong(0);
    AtomicLong totalRequest = new AtomicLong(0);

    @KafkaListener(id = "open-to-test", idIsGroup = false, topics = "open-to-test",
            concurrency = "1", containerFactory = Consumer.DEFAULT_KAFKA_CONTAINER)
    public void openToTest(ConsumerRecord<String, String> record) {
        log.info("received message: {}", record);
        Map value = JsonConversionUtility.classFromJsonJavaReflectionSilently(record.value(), Map.class);
        long postingTime = ((Double) value.get("postingTime")).longValue();
        long totalLatency = totalTimeDiff.addAndGet(System.currentTimeMillis() - postingTime);
        long requestCount = totalRequest.incrementAndGet();
        log.info("Request count: {}, totalLatency: {}, Average latency: {}", requestCount, totalLatency, totalLatency / requestCount);
    }

}
