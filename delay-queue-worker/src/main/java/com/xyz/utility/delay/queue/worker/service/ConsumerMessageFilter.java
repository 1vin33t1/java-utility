package com.xyz.utility.delay.queue.worker.service;

import com.xyz.utility.common.constant.UtilityConstant;
import com.xyz.utility.persistence.repository.aerospike.KafkaProcessedIdRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsumerMessageFilter implements RecordFilterStrategy<String, String> {

    private final KafkaProcessedIdRepository kafkaUniqueMessageRepository;

    @Override
    public boolean filter(ConsumerRecord<String, String> record) {
        try {
            Header kfidHeader = record.headers().lastHeader(UtilityConstant.Kafka.MESSAGE_ID);
            if (kfidHeader != null && kfidHeader.value() != null) {
                final boolean exists = kafkaUniqueMessageRepository.existsById(new String(kfidHeader.value()));
                if (exists)
                    log.warn("duplicate message detected in the message filter, record : {}", record);
                return exists;
            }
        } catch (Exception suppressedException) {
            log.error("Error while filtering the message : {}", record, suppressedException);
        }
        return false;
    }
}
