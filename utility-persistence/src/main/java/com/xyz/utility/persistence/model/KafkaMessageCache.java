package com.xyz.utility.persistence.model;

import lombok.Data;
import org.springframework.data.aerospike.mapping.Document;
import org.springframework.data.annotation.Id;

import java.util.Map;

@Data
@Document(collection = "${aerospike.set.kafka-message-cache.name}", expirationExpression = "${aerospike.set.kafka-message.ttl}")
public class KafkaMessageCache {
    @Id
    private String id;
    private String topic;
    private String key;
    private Object value;
    private Map<String, String> headers;
    private long postTime;
    private String failurePostCallbackUrl;
    private int tries;
}
