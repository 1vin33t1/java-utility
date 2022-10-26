package com.xyz.utility.common.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProducerFailureHandlerImpl implements ProducerFailureHandler {

    public void failureHandler(ProducerRecord<String, Object> record, Exception ex) {
        log.error("Failed event, record: {}, exception: {}", record, ex);
    }
}
