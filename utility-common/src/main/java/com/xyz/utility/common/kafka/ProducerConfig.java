package com.xyz.utility.common.kafka;

import com.xyz.utility.common.constant.UtilityConstant;
import io.opentracing.contrib.kafka.TracingProducerInterceptor;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(value = "kafka.producer.enable", havingValue = "true")
public class ProducerConfig {

    private final ProducerFailureHandler producerFailureHandler;

    @Value("${kafka.producer.bootstrap.servers}")
    private String bootstrapServer;
    @Value("${kafka.producer.retries.config}")
    private int retries;
    @Value("${kafka.producer.reconnect.backoff.ms}")
    private String reconnectBackoffMs;
    @Value("${kafka.producer.batch.size}")
    private String batchSize;
    @Value("${kafka.producer.linger-ms.config}")
    private String lingerMs;
    @Value("${kafka.producer.transaction-timeout.config}")
    private String transactionTimeoutMs;
    @Value("${kafka.producer.acks.config}")
    private String acksConfig;


    @Bean(name = UtilityConstant.Kafka.JSON_KAFKA_TEMPLATE)
    public KafkaTemplate<String, Object> jsonKafkaTemplate() {
        final DefaultKafkaProducerFactory<String, Object> producerFactory = new DefaultKafkaProducerFactory<>(buildKafkaProperties(), new StringSerializer(), new JsonSerializer<>());
        final KafkaTemplate<String, Object> stringObjectKafkaTemplate = new KafkaTemplate<>(producerFactory);
        stringObjectKafkaTemplate.setProducerListener(new DefaultProducerListener(producerFailureHandler));
        return stringObjectKafkaTemplate;
    }

    private Map<String, Object> buildKafkaProperties() {
        final Map<String, Object> properties = new HashMap<>();
        properties.put(org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        properties.put(org.apache.kafka.clients.producer.ProducerConfig.RETRIES_CONFIG, retries);
        properties.put(org.apache.kafka.clients.producer.ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, reconnectBackoffMs);
        properties.put(org.apache.kafka.clients.producer.ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
        properties.put(org.apache.kafka.clients.producer.ProducerConfig.LINGER_MS_CONFIG, lingerMs);
        properties.put(org.apache.kafka.clients.producer.ProducerConfig.TRANSACTION_TIMEOUT_CONFIG, transactionTimeoutMs);
        properties.put(org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG, acksConfig);
        properties.put(org.apache.kafka.clients.producer.ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, Collections.singletonList(TracingProducerInterceptor.class));
        return properties;
    }

}
