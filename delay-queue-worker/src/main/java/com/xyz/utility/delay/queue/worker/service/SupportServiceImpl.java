package com.xyz.utility.delay.queue.worker.service;

import com.xyz.utility.common.constant.UtilityConstant;
import com.xyz.utility.common.kafka.dto.DelayMessageRequest;
import com.xyz.utility.common.kafka.dto.LoadRequest;
import com.xyz.utility.common.util.HashUtil;
import com.xyz.utility.common.util.JsonConversionUtility;
import com.xyz.utility.delay.queue.worker.config.RuntimeProperties;
import com.xyz.utility.persistence.model.KafkaMessageCache;
import com.xyz.utility.persistence.repository.aerospike.KafkaProcessedIdRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportServiceImpl implements SupportService {

    private final DelayQueueService delayQueueService;
    private final RuntimeProperties runtimeProperties;
    private final KafkaProcessedIdRepository kafkaProcessedIdRepository;

    public void saveToDelayDb(String messageId, String key, DelayMessageRequest request) {
        validationSaveToDelayDb(messageId, key, request);
        if (!isDuplicate(messageId)) {
            Map<String, Object> objHeaders = new HashMap<>();
            Optional.ofNullable(request.getHeaders()).ifPresent(headers -> headers.forEach(objHeaders::put));
            MessageHeaders messageHeaders = new MessageHeaders(objHeaders);
            KafkaMessageCache messageCache;
            if (request.getTopic().equals(runtimeProperties.getDelayKafkaTopic())) {
                messageCache = JsonConversionUtility.convertObject(request.getValue(), KafkaMessageCache.class);
            } else {
                messageCache = JsonConversionUtility.convertObject(request, KafkaMessageCache.class);
                messageCache.setPostTime(System.currentTimeMillis() + runtimeProperties.getDelayServiceAerospikeSaveTimeMs());
            }
            delayQueueService.preProcess(messageId, messageHeaders, messageCache);
        }
    }

    private void validationSaveToDelayDb(String messageId, String key, DelayMessageRequest request) {
        if (request == null || Arrays.asList(messageId, key, request.getTopic()).contains(null))
            throw new IllegalArgumentException(UtilityConstant.SAVE_TO_DELAY_DB_VALIDATION_FAILED);
        String calculatedHash = HashUtil.sha512(List.of(messageId, runtimeProperties.getDelayServiceAerospikeSaveSalt()), UtilityConstant.HASH_DELIMITER);
        if (!key.equals(calculatedHash))
            throw new IllegalArgumentException(UtilityConstant.HASH_VALIDATION_FAILED);
    }

    private boolean isDuplicate(String messageId) {
        try {
            if (kafkaProcessedIdRepository.existsById(messageId)) {
                log.warn("support service detected a duplicate event : {}", messageId);
                return true;
            }
        } catch (Exception ex) {
            log.error("Error while checking existence of processed-id {} in aerospike", messageId, ex);
        }
        return false;
    }

    @Override
    public void saveToDelayDb(int count) {
        IntStream.range(0, count).forEach(it -> {
            KafkaMessageCache messageCache = new KafkaMessageCache();
            messageCache.setTopic("open-to-test");
            messageCache.setPostTime(System.currentTimeMillis() + 1000);
            messageCache.setKey(String.valueOf(System.currentTimeMillis()));
            LoadRequest lr = new LoadRequest(System.currentTimeMillis());
            messageCache.setValue(lr);
            messageCache.setHeaders(new HashMap<>());
            MessageHeaders messageHeaders = new MessageHeaders(new HashMap<>());
            delayQueueService.preProcess(String.valueOf(System.nanoTime()), messageHeaders, messageCache);
        });

    }
}
