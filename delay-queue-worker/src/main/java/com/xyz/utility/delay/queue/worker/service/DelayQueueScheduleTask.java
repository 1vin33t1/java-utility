package com.xyz.utility.delay.queue.worker.service;

import com.xyz.utility.common.constant.UtilityConstant;
import com.xyz.utility.common.kafka.ProducerTemplate;
import com.xyz.utility.delay.queue.worker.dto.AerospikeEventValue;
import com.xyz.utility.persistence.repository.aerospike.KafkaMessageCacheRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@Slf4j
@Service
@ConditionalOnProperty(value = "scheduler.enable", havingValue = "true")
public class DelayQueueScheduleTask {

    @Value("${kafka.post.delay.topic.name}")
    private String postDelayTopic;
    private final ProducerTemplate producerTemplate;
    private final KafkaMessageCacheRepository kafkaMessageCacheRepository;
    private final ConcurrentTaskScheduler scheduler;

    public DelayQueueScheduleTask(ProducerTemplate producerTemplate,
                                  KafkaMessageCacheRepository kafkaMessageCacheRepository,
                                  @Value("${scheduler.pool.size}") int poolSize) {
        this.producerTemplate = producerTemplate;
        this.kafkaMessageCacheRepository = kafkaMessageCacheRepository;
        this.scheduler = new ConcurrentTaskScheduler(new ScheduledThreadPoolExecutor(poolSize));
        if (1 == 1) {
            scheduler.scheduleAtFixedRate(this::rescueNearbyDelayQueueTask, Instant.now().plusSeconds(5), Duration.ofSeconds(10));
            scheduler.scheduleAtFixedRate(this::rescueOldDelayQueueTask, Instant.now().plusSeconds(5), Duration.ofMinutes(30));
        }
    }

    public void rescueNearbyDelayQueueTask() {
        log.info("rescue task initiated");
        kafkaMessageCacheRepository.findAllByPostTimeBetween(Instant.now().minusSeconds(120).toEpochMilli(),
                Instant.now().minusSeconds(10).toEpochMilli())
                .forEach(messageCache -> {
                    log.info("rescue task found dropped task: {}", messageCache.getId());
                    pushIdToKafka(messageCache.getId());
                });
        log.info("rescue task finished");
    }

    public void rescueOldDelayQueueTask() {
        log.info("rescue task initiated");
        kafkaMessageCacheRepository.findAllByPostTimeLessThan(Instant.now().minus(2, ChronoUnit.HOURS).toEpochMilli())
                .forEach(messageCache -> {
                    log.info("rescue task found dropped task: {}", messageCache.getId());
                    pushIdToKafka(messageCache.getId());
                });
        log.info("rescue task finished");
    }

    protected void pushIdToKafka(String id) {
        log.info("Sending id to kafka for rescue task : {}", id);
        AerospikeEventValue aerospikeEventValue = new AerospikeEventValue();
        aerospikeEventValue.setMetadata(new AerospikeEventValue.Metadata());
        aerospikeEventValue.getMetadata().setMsg(UtilityConstant.DELETE_EVENT);
        aerospikeEventValue.getMetadata().setUserKey(id);
        RecordHeaders headers = new RecordHeaders();
        producerTemplate.send(new ProducerRecord<>(postDelayTopic, null, System.currentTimeMillis(),
                String.valueOf(System.currentTimeMillis()), aerospikeEventValue, headers));
    }

}
