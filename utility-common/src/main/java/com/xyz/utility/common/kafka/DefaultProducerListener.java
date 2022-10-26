package com.xyz.utility.common.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.support.ProducerListener;

@Slf4j
@RequiredArgsConstructor
public class DefaultProducerListener implements ProducerListener<String, Object> {

    private final ProducerFailureHandler producerFailureHandler;

    @Override
    public void onError(ProducerRecord<String, Object> producerRecord, RecordMetadata recordMetadata, Exception exception) {
        producerFailureHandler.failureHandler(producerRecord, exception);
    }

    @Override
    public void onSuccess(ProducerRecord<String, Object> producerRecord, RecordMetadata recordMetadata) {
        log.info("Successful event, record: {}, metadata: {}", producerRecord, recordMetadata);
    }

}
