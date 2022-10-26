package com.xyz.utility.persistence.repository.aerospike;

import com.xyz.utility.persistence.model.KafkaProcessedId;
import org.springframework.data.repository.CrudRepository;

public interface KafkaProcessedIdRepository extends CrudRepository<KafkaProcessedId, String> {
}
