package com.linkwave.app.service.kafka;

import com.linkwave.app.domain.chat.ChatEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service for publishing chat events to Kafka.
 * 
 * Phase C2: Direct Kafka integration
 * - Publishes ChatEvent to linkwave.chat.events topic
 * - Partitions by recipient phone number for ordering guarantees
 * - Idempotent producer for exactly-once semantics
 * 
 * Phase C3: Will add delivery confirmation and retry logic
 */
@Service
public class ChatEventProducer {
    
    private static final Logger log = LoggerFactory.getLogger(ChatEventProducer.class);
    
    private static final String CHAT_EVENTS_TOPIC = "linkwave.chat.events";
    
    private final KafkaTemplate<String, ChatEvent> kafkaTemplate;
    
    public ChatEventProducer(KafkaTemplate<String, ChatEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    /**
     * Publish chat event to Kafka.
     * Partitions by recipient phone number to ensure ordering.
     * 
     * @param event the chat event to publish
     * @return completable future with send result
     */
    public CompletableFuture<SendResult<String, ChatEvent>> publishChatEvent(ChatEvent event) {
        // Use recipient as partition key for ordering
        String partitionKey = event.getRecipient();
        
        log.info("Publishing chat event: messageId={}, sender={}, recipient={}, bodyLength={}", 
                 event.getMessageId(), 
                 event.getMaskedSender(), 
                 event.getMaskedRecipient(),
                 event.getBody() != null ? event.getBody().length() : 0);
        
        CompletableFuture<SendResult<String, ChatEvent>> future = 
            kafkaTemplate.send(CHAT_EVENTS_TOPIC, partitionKey, event);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                var metadata = result.getRecordMetadata();
                log.info("Chat event published successfully: messageId={}, topic={}, partition={}, offset={}", 
                         event.getMessageId(), 
                         metadata.topic(), 
                         metadata.partition(), 
                         metadata.offset());
            } else {
                log.error("Failed to publish chat event: messageId={}, sender={}, recipient={}, error={}", 
                          event.getMessageId(), 
                          event.getMaskedSender(), 
                          event.getMaskedRecipient(), 
                          ex.getMessage());
            }
        });
        
        return future;
    }
    
    /**
     * Get the chat events topic name.
     * 
     * @return topic name
     */
    public String getChatEventsTopic() {
        return CHAT_EVENTS_TOPIC;
    }
}
