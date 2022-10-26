package com.xyz.utility.delay.queue.worker.service;

import com.xyz.utility.common.constant.UtilityConstant;
import com.xyz.utility.common.kafka.ProducerFailureHandler;
import com.xyz.utility.common.util.CommonUtility;
import com.xyz.utility.persistence.model.KafkaMessageCache;
import com.xyz.utility.persistence.repository.aerospike.KafkaMessageIdAndCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Primary
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "retry.producer.enable", havingValue = "true")
public class RetryProducerFailure implements ProducerFailureHandler {

    private final KafkaMessageIdAndCacheRepository kafkaMessageIdAndCacheRepository;
    private final DelayFailureHandler failureHandler;

    public void failureHandler(ProducerRecord<String, Object> record, Exception ex) {
        KafkaMessageCache kafkaMessageCache = new KafkaMessageCache();
        kafkaMessageCache.setId(CommonUtility.generateId(record.topic(), record.key()));
        kafkaMessageCache.setKey(record.key());
        kafkaMessageCache.setTopic(record.topic());
        kafkaMessageCache.setValue(record.value());
        Map<String, String> mapHeaders = new HashMap<>();
        kafkaMessageCache.setHeaders(mapHeaders);
        record.headers().forEach(header -> {
            String strVal = "";
            if (header.value() != null)
                strVal = new String(header.value());
            mapHeaders.put(header.key(), strVal);
        });
        if (mapHeaders.containsKey(UtilityConstant.Kafka.FAILURE_CALLBACK_URL))
            kafkaMessageCache.setFailurePostCallbackUrl(mapHeaders.get(UtilityConstant.Kafka.FAILURE_CALLBACK_URL));
        try {
            kafkaMessageIdAndCacheRepository.save(kafkaMessageCache, 1);
        } catch (Exception exception) {
            log.error("Failed event, record: {}, exception: {}", record, ex);
            failureHandler.notifyClient(kafkaMessageCache, UtilityConstant.Kafka.KAFKA_SEND_FAILED, ex);
        }
    }
}
