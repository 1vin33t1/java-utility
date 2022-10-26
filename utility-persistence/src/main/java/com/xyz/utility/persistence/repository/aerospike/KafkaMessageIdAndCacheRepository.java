package com.xyz.utility.persistence.repository.aerospike;

import com.aerospike.client.policy.WritePolicy;
import com.xyz.utility.persistence.model.KafkaMessageCache;
import com.xyz.utility.persistence.model.KafkaMessageId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.aerospike.core.AerospikeTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(value = "aerospike.enable", havingValue = "true")
public class KafkaMessageIdAndCacheRepository {

    private final AerospikeTemplate aerospikeTemplate;
    @Value("${aerospike.set.kafka-message.ttl}")
    private int kafkaMessageCacheTtl;

    public void save(KafkaMessageCache messageCache, int ttl) {
        if (ttl == 0)
            ttl = 1;
        WritePolicy messagePolicy = new WritePolicy();
        messagePolicy.expiration = kafkaMessageCacheTtl + ttl;
        WritePolicy messageIdPolicy = new WritePolicy();
        messageIdPolicy.expiration = ttl;
        messageIdPolicy.sendKey = true;
        if (messageCache.getId() == null)
            messageCache.setId(UUID.randomUUID().toString());
        aerospikeTemplate.persist(messageCache, messagePolicy);
        aerospikeTemplate.persist(new KafkaMessageId(messageCache.getId()), messageIdPolicy);
    }

    public void deleteById(String id) {
        aerospikeTemplate.delete(id, KafkaMessageCache.class);
        aerospikeTemplate.delete(id, KafkaMessageId.class);
    }

}
