package com.xyz.utility.common.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;

public interface ProducerFailureHandler {
    void failureHandler(ProducerRecord<String, Object> record, Exception ex);
}
