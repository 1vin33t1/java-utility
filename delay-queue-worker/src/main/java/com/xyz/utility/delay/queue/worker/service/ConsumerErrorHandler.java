package com.xyz.utility.delay.queue.worker.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.ErrorHandler;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ConsumerErrorHandler implements ErrorHandler {

    @Override
    public void handle(Exception e, ConsumerRecord<?, ?> consumerRecord) {
        log.error("Exception occurred while reading message : {}", consumerRecord, e);
    }

    @Override
    public boolean isAckAfterHandle() {
        return true;
    }
}
