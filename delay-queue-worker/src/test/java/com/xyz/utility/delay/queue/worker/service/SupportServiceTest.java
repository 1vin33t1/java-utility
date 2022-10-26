package com.xyz.utility.delay.queue.worker.service;

import com.xyz.utility.common.kafka.dto.DelayMessageRequest;
import com.xyz.utility.delay.queue.worker.config.RuntimeProperties;
import com.xyz.utility.persistence.model.KafkaMessageCache;
import com.xyz.utility.persistence.repository.aerospike.KafkaProcessedIdRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessageHeaders;

import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class SupportServiceTest {

    @Mock
    private DelayQueueService delayQueueService;
    @Mock
    private RuntimeProperties runtimeProperties;
    @Mock
    private KafkaProcessedIdRepository kafkaProcessedIdRepository;
    @InjectMocks
    private SupportServiceImpl supportService;

    @Test
    void saveToDelayDb_validationFailed_1() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> supportService.saveToDelayDb(null, null, null));
    }

    @Test
    void saveToDelayDb_validationFailed_2() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> supportService.saveToDelayDb(null, null, new DelayMessageRequest()));
    }

    @Test
    void saveToDelayDb_validationFailed_3() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> supportService.saveToDelayDb("123", "123", new DelayMessageRequest()));
    }

    @Test
    void saveToDelayDb_hashFailed() {
        Mockito.when(runtimeProperties.getDelayServiceAerospikeSaveSalt()).thenReturn("1");
        var request = new DelayMessageRequest();
        request.setTopic("123");
        Assertions.assertThrows(IllegalArgumentException.class, () -> supportService.saveToDelayDb("123", "123", request));
    }

    @Test
    void saveToDelayDb_delayTopic() {
        Mockito.when(runtimeProperties.getDelayServiceAerospikeSaveSalt()).thenReturn("1");
        Mockito.when(runtimeProperties.getDelayKafkaTopic()).thenReturn("123");
        Mockito.when(kafkaProcessedIdRepository.existsById("123")).thenReturn(false);
        var request = new DelayMessageRequest();
        request.setTopic("123");
        request.setHeaders(new HashMap<>());
        request.getHeaders().put("123", "123");
        request.getHeaders().put("124", null);
        var value = new DelayMessageRequest();
        value.setTopic("234");
        request.setValue(value);
        var captor = ArgumentCaptor.forClass(KafkaMessageCache.class);
        supportService.saveToDelayDb("123", "dd4f162a5a8ab7d8c2ee9563cfd891d04f07b79876b7e3b38e5c68d1f2fef4eb6d3f7c5438dfaf4000b6ea8990996fb8497fd69f39f39ea4c8ccf3bb88db1190", request);
        Mockito.verify(delayQueueService).preProcess(eq("123"), any(MessageHeaders.class), captor.capture());
        var req = captor.getValue();
        Assertions.assertEquals("234", req.getTopic());
    }

    @Test
    void saveToDelayDb_normalTopic() {
        Mockito.when(runtimeProperties.getDelayServiceAerospikeSaveSalt()).thenReturn("1");
        Mockito.when(runtimeProperties.getDelayKafkaTopic()).thenReturn("delay");
        Mockito.when(runtimeProperties.getDelayServiceAerospikeSaveTimeMs()).thenReturn(1000);
        Mockito.when(kafkaProcessedIdRepository.existsById("123")).thenReturn(false);
        var request = new DelayMessageRequest();
        request.setTopic("123");
        var captor = ArgumentCaptor.forClass(KafkaMessageCache.class);
        supportService.saveToDelayDb("123", "dd4f162a5a8ab7d8c2ee9563cfd891d04f07b79876b7e3b38e5c68d1f2fef4eb6d3f7c5438dfaf4000b6ea8990996fb8497fd69f39f39ea4c8ccf3bb88db1190", request);
        Mockito.verify(delayQueueService).preProcess(eq("123"), any(MessageHeaders.class), captor.capture());
        KafkaMessageCache req = captor.getValue();
        Assertions.assertEquals("123", req.getTopic());
    }

    @Test
    public void saveToDelayDb_skipDuplicate() {
        Mockito.when(runtimeProperties.getDelayServiceAerospikeSaveSalt()).thenReturn("1");
        Mockito.when(kafkaProcessedIdRepository.existsById("123")).thenReturn(true);
        var request = new DelayMessageRequest();
        request.setTopic("123");
        supportService.saveToDelayDb("123", "dd4f162a5a8ab7d8c2ee9563cfd891d04f07b79876b7e3b38e5c68d1f2fef4eb6d3f7c5438dfaf4000b6ea8990996fb8497fd69f39f39ea4c8ccf3bb88db1190", request);
        Mockito.verifyNoInteractions(delayQueueService);
    }

    @Test
    public void saveToDelayDb_errorOnCheckingDuplicate() {
        Mockito.when(runtimeProperties.getDelayServiceAerospikeSaveSalt()).thenReturn("1");
        Mockito.when(runtimeProperties.getDelayKafkaTopic()).thenReturn("delay");
        Mockito.when(runtimeProperties.getDelayServiceAerospikeSaveTimeMs()).thenReturn(1000);
        Mockito.when(kafkaProcessedIdRepository.existsById("123")).thenThrow(new RuntimeException("123"));
        var request = new DelayMessageRequest();
        request.setTopic("123");
        var captor = ArgumentCaptor.forClass(KafkaMessageCache.class);
        supportService.saveToDelayDb("123", "dd4f162a5a8ab7d8c2ee9563cfd891d04f07b79876b7e3b38e5c68d1f2fef4eb6d3f7c5438dfaf4000b6ea8990996fb8497fd69f39f39ea4c8ccf3bb88db1190", request);
        Mockito.verify(delayQueueService).preProcess(eq("123"), any(MessageHeaders.class), captor.capture());
        KafkaMessageCache req = captor.getValue();
        Assertions.assertEquals("123", req.getTopic());
    }

}