package com.xyz.utility.persistence.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.aerospike.mapping.Document;
import org.springframework.data.annotation.Id;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "${aerospike.set.kafka-processed-id.name}", expirationExpression = "${aerospike.set.kafka-processed-id.ttl}")
public class KafkaProcessedId {
    @Id
    private String id;
}
