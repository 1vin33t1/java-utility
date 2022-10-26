package com.xyz.utility.delay.queue.worker.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class RuntimeProperties {

    @Value("${kafka.pre.delay.topic.tolerance.time}")
    private long toleranceTime;
    @Value("${kafka.pre.delay.topic.tries.max.count}")
    private int triesMaxCount;
    @Value("${kafka.pre.delay.topic.name}")
    private String delayKafkaTopic;
    @Value("${kafka.post.delay.topic.name}")
    private String postDelayKafkaTopicName;

    @Value("${java.utility.security.user.name}")
    private String appUserName;
    @Value("${java.utility.security.user.auth}")
    private String appAuthentication;
    @Value("${delay.service.aerospike.http.save.salt}")
    private String delayServiceAerospikeSaveSalt;
    @Value("${delay.service.aerospike.http.save.delay.ms}")
    private int delayServiceAerospikeSaveTimeMs;

}
