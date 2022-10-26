package com.xyz.utility.delay.queue.worker.service;

import com.xyz.utility.persistence.model.KafkaProcessedId;
import com.xyz.utility.persistence.repository.aerospike.KafkaProcessedIdRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KafkaUniqueMessageAspectTest {

    private KafkaUniqueMessageAspect kafkaUniqueMessageAspect;
    private KafkaProcessedIdRepository kafkaProcessedIdRepository;

    @BeforeEach
    public void setUp() {
        kafkaProcessedIdRepository = mock(KafkaProcessedIdRepository.class);
        kafkaUniqueMessageAspect = new KafkaUniqueMessageAspect(kafkaProcessedIdRepository);
    }

    @Test
    public void afterReturning_shouldSave() {
        kafkaUniqueMessageAspect.afterReturning(null, "123");
        verify(kafkaProcessedIdRepository).save(new KafkaProcessedId("123"));
    }

    @Test
    public void afterReturning_shouldNotSave() {
        kafkaUniqueMessageAspect.afterReturning(null, null);
        verify(kafkaProcessedIdRepository, never()).save(new KafkaProcessedId(null));
    }

    @Test
    public void afterReturning_errorInSaving() {
        Mockito.when(kafkaProcessedIdRepository.save(any())).thenReturn(new RuntimeException("123"));
        kafkaUniqueMessageAspect.afterReturning(null, "123");
    }

}