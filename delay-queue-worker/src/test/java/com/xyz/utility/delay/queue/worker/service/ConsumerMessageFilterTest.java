package com.xyz.utility.delay.queue.worker.service;


import com.xyz.utility.common.constant.UtilityConstant;
import com.xyz.utility.common.util.CommonUtility;
import com.xyz.utility.persistence.repository.aerospike.KafkaProcessedIdRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class ConsumerMessageFilterTest {
    private ConsumerMessageFilter consumerMessageFilter;
    ConsumerRecord<String, String> uniqueRecord;
    ConsumerRecord<String, String> duplicateRecord;
    ConsumerRecord<String, String> errorRecord;

    @BeforeEach
    public void setUp() {
        KafkaProcessedIdRepository kafkaProcessedIdRepository = mock(KafkaProcessedIdRepository.class);
        consumerMessageFilter = new ConsumerMessageFilter(kafkaProcessedIdRepository);

        uniqueRecord = new ConsumerRecord<>("test", 0, 0, "test-key1", "test-value1");
        String uniqueId = CommonUtility.generateId(uniqueRecord.topic(), uniqueRecord.key());
        uniqueRecord.headers().add(UtilityConstant.Kafka.MESSAGE_ID, uniqueId.getBytes(StandardCharsets.UTF_8));
        Mockito.lenient().when(kafkaProcessedIdRepository.existsById(uniqueId)).thenReturn(false);

        duplicateRecord = new ConsumerRecord<>("test", 0, 1, "test-key2", "test-value2");
        String dupId = CommonUtility.generateId(duplicateRecord.topic(), duplicateRecord.key());
        duplicateRecord.headers().add(UtilityConstant.Kafka.MESSAGE_ID, dupId.getBytes(StandardCharsets.UTF_8));
        Mockito.lenient().when(kafkaProcessedIdRepository.existsById(dupId)).thenReturn(true);

        errorRecord = new ConsumerRecord<>("test", 0, 0, "test-key1", "test-value1");
        String errorId = CommonUtility.generateId(errorRecord.topic(), errorRecord.key());
        errorRecord.headers().add(UtilityConstant.Kafka.MESSAGE_ID, errorId.getBytes(StandardCharsets.UTF_8));
        errorRecord.headers().add("123", null);
        Mockito.lenient().when(kafkaProcessedIdRepository.existsById(errorId)).thenThrow(new RuntimeException("123"));
    }

    @Test
    public void filter_pass() {
        assertFalse(consumerMessageFilter.filter(uniqueRecord));
    }

    @Test
    public void filter_reject() {
        assertTrue(consumerMessageFilter.filter(duplicateRecord));
    }

    @Test
    public void filter_withExceptionPass() {
        assertFalse(consumerMessageFilter.filter(errorRecord));
    }

    @Test
    public void filter_noKafkaHeaderPass() {
        ConsumerRecord<String, String> testRecord = new ConsumerRecord<>("test", 0, 0, "test-key1", "test-value1");
        assertFalse(consumerMessageFilter.filter(testRecord));
    }

    @Test
    public void filter_nullKafkaHeaderPass() {
        ConsumerRecord<String, String> testRecord = new ConsumerRecord<>("test", 0, 0, "test-key1", "test-value1");
        testRecord.headers().add(UtilityConstant.Kafka.MESSAGE_ID, null);
        assertFalse(consumerMessageFilter.filter(testRecord));
    }

}