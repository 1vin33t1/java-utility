package com.xyz.utility.delay.queue.worker.service;

import com.xyz.utility.delay.queue.worker.annotation.DuplicateMessageDetector;
import com.xyz.utility.persistence.model.KafkaProcessedId;
import com.xyz.utility.persistence.repository.aerospike.KafkaProcessedIdRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Service;

@Aspect
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaUniqueMessageAspect {

    private final KafkaProcessedIdRepository kafkaProcessedIdRepository;

    @After(value = "@annotation(com.xyz.utility.delay.queue.worker.annotation.DuplicateMessageDetector) && @annotation(duplicateMessageDetector) && args(id,..)",
            argNames = "duplicateMessageDetector,id")
    public void afterReturning(final DuplicateMessageDetector duplicateMessageDetector, String id) {
        try {
            if (id != null)
                kafkaProcessedIdRepository.save(new KafkaProcessedId(id));
        } catch (Exception suppressedException) {
            log.error("Exception while saving the message id in the aerospike :{}", id, suppressedException);
        }
    }

}
