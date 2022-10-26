package com.xyz.utility.common.kafka;

import com.xyz.utility.common.constant.UtilityConstant;
import com.xyz.utility.common.kafka.dto.DelayMessageRequest;
import com.xyz.utility.common.util.CommonUtility;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.concurrent.ListenableFuture;

import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class ProducerTemplate {

    private final KafkaTemplate<String, Object> template;
    private final String delayTopicName;
    private ListenableFuture<SendResult<String, Object>> lastFuture;

    public ProducerTemplate(@Qualifier(UtilityConstant.Kafka.JSON_KAFKA_TEMPLATE) KafkaTemplate<String, Object> template,
                            @Value("${kafka.pre.delay.topic.name}") String delayTopicName) {
        this.template = template;
        this.delayTopicName = delayTopicName;
    }

    public ListenableFuture<SendResult<String, Object>> send(ProducerRecord<String, Object> record) {
        log.info("pushing message to kafka, record: {}", record);
        if (record.headers().lastHeader(UtilityConstant.Kafka.MESSAGE_ID) == null) {
            record.headers().add(UtilityConstant.Kafka.MESSAGE_ID,
                    CommonUtility.generateId(record.topic(), record.key()).getBytes(StandardCharsets.UTF_8));
        }
        final ListenableFuture<SendResult<String, Object>> send = template.send(record);
        this.lastFuture = send;
        return send;
    }

    public ListenableFuture<SendResult<String, Object>> send(String topic, String key, Object data, Headers headers) {
        return this.send(new ProducerRecord<>(topic, null, System.currentTimeMillis(), key, data, headers));
    }

    public ListenableFuture<SendResult<String, Object>> send(String topic, String key, Object data) {
        return this.send(topic, key, data, null);
    }

    public ListenableFuture<SendResult<String, Object>> send(String topic, Object data) {
        return this.send(topic, String.valueOf(System.currentTimeMillis()), data);
    }

    public ListenableFuture<SendResult<String, Object>> delaySend(String topic, String key, Object data, Map<String, String> header, String failurePostCallbackUrl, long delay) {
        // sanity check if message is eligible for delay
        if (delay > 0) {
            DelayMessageRequest messageRequest = new DelayMessageRequest(topic, key, data, header,
                    System.currentTimeMillis() + delay, failurePostCallbackUrl);
            return this.send(delayTopicName, messageRequest.getKey(), messageRequest);
        } else {
            Headers headers = null;
            if (!CollectionUtils.isEmpty(header)) {
                Headers finalHeaders = new RecordHeaders();
                header.forEach((k, v) -> finalHeaders.add(k, v != null ? v.getBytes(StandardCharsets.UTF_8) : new byte[0]));
                headers = finalHeaders;
            }
            return this.send(new ProducerRecord<>(topic, null, System.currentTimeMillis(), key, data, headers));
        }
    }

    public ListenableFuture<SendResult<String, Object>> delaySend(String topic, String key, Object data, Map<String, String> header, long delay) {
        return this.delaySend(topic, key, data, header, null, delay);
    }

    public ListenableFuture<SendResult<String, Object>> delaySend(String topic, String key, Object data, long delay) {
        return this.delaySend(topic, key, data, null, delay);
    }

    public ListenableFuture<SendResult<String, Object>> delaySend(String topic, Object data, long delay) {
        return this.delaySend(topic, String.valueOf(System.currentTimeMillis()), data, delay);
    }

    // This is to wait for last inflight request to finish
    @PreDestroy
    public void preDestroy() {
        try {
            if (lastFuture != null)
                lastFuture.get(1000, TimeUnit.MILLISECONDS);
            template.flush();
        } catch (InterruptedException ex) {
            log.error("interrupted exception while waiting for the last kafka message in producer template", ex);
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException ex) {
            log.error("exception while waiting for the last kafka message in producer template", ex);
        }
    }
}
