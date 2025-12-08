package com.tablebanking.loanmanagement.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.support.serializer.JsonSerializer;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Configuration
@Slf4j
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.enabled:true}")
    private boolean kafkaEnabled;

    @Bean
    @Primary
    public KafkaTemplate<String, Object> kafkaTemplate() {
        if (!kafkaEnabled) {
            log.info("Kafka is disabled. Using no-op KafkaTemplate.");
            return new NoOpKafkaTemplate<>();
        }
        
        try {
            Map<String, Object> configProps = new HashMap<>();
            configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
            configProps.put(ProducerConfig.ACKS_CONFIG, "all");
            configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
            configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
            configProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);
            configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

            ProducerFactory<String, Object> producerFactory = new DefaultKafkaProducerFactory<>(configProps);
            log.info("Kafka producer configured with bootstrap servers: {}", bootstrapServers);
            return new KafkaTemplate<>(producerFactory);
        } catch (Exception e) {
            log.warn("Failed to create Kafka producer, using no-op template: {}", e.getMessage());
            return new NoOpKafkaTemplate<>();
        }
    }

    /**
     * No-op KafkaTemplate that does nothing - used when Kafka is disabled or unavailable
     */
    private static class NoOpKafkaTemplate<K, V> extends KafkaTemplate<K, V> {
        
        public NoOpKafkaTemplate() {
            super(new DefaultKafkaProducerFactory<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class
            )));
        }

        @Override
        public CompletableFuture<SendResult<K, V>> send(String topic, V data) {
            log.debug("NoOpKafkaTemplate: Skipping send to topic {}", topic);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<SendResult<K, V>> send(String topic, K key, V data) {
            log.debug("NoOpKafkaTemplate: Skipping send to topic {} with key {}", topic, key);
            return CompletableFuture.completedFuture(null);
        }
    }
}
