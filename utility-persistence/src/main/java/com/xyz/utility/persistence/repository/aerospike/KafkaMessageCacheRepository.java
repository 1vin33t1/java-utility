package com.xyz.utility.persistence.repository.aerospike;

import com.xyz.utility.persistence.model.KafkaMessageCache;
import org.springframework.data.aerospike.repository.AerospikeRepository;

import java.util.List;

public interface KafkaMessageCacheRepository extends AerospikeRepository<KafkaMessageCache, String> {
    List<KafkaMessageCache> findAllByPostTimeLessThan(long postTime);

    List<KafkaMessageCache> findAllByPostTimeBetween(long start, long end);
}
