package com.xyz.utility.delay.queue.worker.config;

import com.xyz.utility.delay.queue.worker.service.ConsumerMessageFilter;
import io.opentracing.contrib.kafka.TracingConsumerInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ErrorHandler;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@EnableKafka
@Configuration
@RequiredArgsConstructor
public class Consumer {

    public static final String DEFAULT_KAFKA_CONTAINER = "kafkaListenerContainerFactory";
    public static final String JAEGER_KAFKA_CONTAINER = "jaegerKafkaListenerContainerFactory";
    private final ConsumerMessageFilter messageFilter;
    private final ErrorHandler errorHandler;
    @Value("${kafka.consumer.bootstrap.servers}")
    private String bootstrapServer;
    @Value("${kafka.consumer.group.name}")
    private String consumerGroup;
    @Value("${kafka.consumer.max.poll.size}")
    private String consumerMaxPollSize;


    @Bean(DEFAULT_KAFKA_CONTAINER)
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(defaultConsumerProps(),
                new StringDeserializer(), new StringDeserializer());
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setRecordFilterStrategy(messageFilter);
        factory.setMessageConverter(new StringJsonMessageConverter());
        factory.setErrorHandler(errorHandler);
        return factory;
    }

    @Bean(JAEGER_KAFKA_CONTAINER)
    public ConcurrentKafkaListenerContainerFactory<String, String> jaegerKafkaListenerContainerFactory() {
        ConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(jaegerConsumerProps(),
                new StringDeserializer(), new StringDeserializer());
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setRecordFilterStrategy(messageFilter);
        factory.setMessageConverter(new StringJsonMessageConverter());
        factory.setErrorHandler(errorHandler);
        return factory;
    }

    private Map<String, Object> defaultConsumerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, consumerMaxPollSize);
        return props;
    }

    private Map<String, Object> jaegerConsumerProps() {
        Map<String, Object> props = defaultConsumerProps();
        props.put(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, TracingConsumerInterceptor.class.getName());
        return props;
    }
}
