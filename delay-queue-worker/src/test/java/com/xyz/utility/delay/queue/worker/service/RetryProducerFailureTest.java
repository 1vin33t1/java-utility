package com.xyz.utility.delay.queue.worker.service;

import com.xyz.utility.common.constant.UtilityConstant;
import com.xyz.utility.persistence.model.KafkaMessageCache;
import com.xyz.utility.persistence.repository.aerospike.KafkaMessageIdAndCacheRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class RetryProducerFailureTest {

    @Mock
    private KafkaMessageIdAndCacheRepository kafkaMessageIdAndCacheRepository;
    @Mock
    private DelayFailureHandler failureHandler;
    @InjectMocks
    private RetryProducerFailure retryProducerFailure;

    @Test
    void failureHandler_success() {
        var record = new ProducerRecord<String, Object>("123", "key", "value");
        retryProducerFailure.failureHandler(record, null);
        Mockito.verify(kafkaMessageIdAndCacheRepository).save(any(), eq(1));
        Mockito.verifyNoInteractions(failureHandler);
    }

    @Test
    void failureHandler_successWithHeaders() {
        RecordHeaders recordHeader = new RecordHeaders();
        recordHeader.add("123", null);
        recordHeader.add(UtilityConstant.Kafka.FAILURE_CALLBACK_URL, "12345".getBytes(StandardCharsets.UTF_8));
        var captor = ArgumentCaptor.forClass(KafkaMessageCache.class);
        var record = new ProducerRecord<String, Object>("123", 0, "key", "value", recordHeader);
        retryProducerFailure.failureHandler(record, null);
        Mockito.verify(kafkaMessageIdAndCacheRepository).save(captor.capture(), eq(1));
        KafkaMessageCache req = captor.getValue();
        Assertions.assertEquals("12345", req.getFailurePostCallbackUrl());
        Mockito.verifyNoInteractions(failureHandler);
    }

    @Test
    void failureHandler_fail() {
        var record = new ProducerRecord<String, Object>("123", "key", "value");
        Mockito.doThrow(new RuntimeException("123")).when(kafkaMessageIdAndCacheRepository).save(any(), eq(1));
        retryProducerFailure.failureHandler(record, null);
        Mockito.verify(failureHandler).notifyClient(any(), any(), eq(null));
    }


}