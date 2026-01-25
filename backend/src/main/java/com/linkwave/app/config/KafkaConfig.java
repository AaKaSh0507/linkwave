package com.linkwave.app.config;

import com.linkwave.app.domain.chat.ChatEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for chat event messaging.
 * 
 * Phase C2: Direct Kafka integration
 * - Producer: Publishes ChatEvent to linkwave.chat.events
 * - Consumer: Receives ChatEvent for future delivery pipeline (C4)
 * - Partition strategy: By recipient phone number for ordering
 * - Replication factor: 1 (local learning environment)
 * 
 * Topics:
 * - linkwave.chat.events: Main chat event stream
 */
@Configuration
@EnableKafka
public class KafkaConfig {
    
    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;
    
    @Value("${spring.kafka.consumer.group-id:linkwave-chat-delivery}")
    private String consumerGroupId;
    
    /**
     * Kafka producer configuration.
     * Idempotent producer enabled for exactly-once semantics.
     */
    @Bean
    public ProducerFactory<String, ChatEvent> chatEventProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // Enable idempotence for exactly-once delivery
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        
        // Error handling
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);
        
        return new DefaultKafkaProducerFactory<>(config);
    }
    
    @Bean
    public KafkaTemplate<String, ChatEvent> chatEventKafkaTemplate() {
        return new KafkaTemplate<>(chatEventProducerFactory());
    }
    
    /**
     * Kafka consumer configuration.
     * Consumer group for delivery pipeline (future C4).
     */
    @Bean
    public ConsumerFactory<String, ChatEvent> chatEventConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        
        // JsonDeserializer configuration
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.linkwave.app.domain.chat");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ChatEvent.class.getName());
        
        // Consumer behavior
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        config.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 5000);
        
        return new DefaultKafkaConsumerFactory<>(config);
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ChatEvent> chatEventKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ChatEvent> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(chatEventConsumerFactory());
        
        // Error handling - log and continue (placeholder for DLQ in future)
        factory.setCommonErrorHandler(new org.springframework.kafka.listener.DefaultErrorHandler());
        
        return factory;
    }
}
