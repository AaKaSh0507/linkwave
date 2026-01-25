package com.linkwave.app.service.kafka;

import com.linkwave.app.domain.chat.ChatEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka consumer for chat events.
 * 
 * Phase C2: Receives events from Kafka and logs them
 * - No delivery to WebSocket recipients yet (C4)
 * - No persistence yet (C3)
 * - Validates receipt and logs for debugging
 * 
 * Consumer group: linkwave-chat-delivery
 * Topic: linkwave.chat.events
 */
@Service
public class ChatEventConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(ChatEventConsumer.class);
    
    /**
     * Consume chat events from Kafka.
     * Phase C2: Log receipt only, no delivery or persistence.
     * 
     * @param record the Kafka consumer record
     */
    @KafkaListener(
        topics = "linkwave.chat.events",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "chatEventKafkaListenerContainerFactory"
    )
    public void consumeChatEvent(ConsumerRecord<String, ChatEvent> record) {
        ChatEvent event = record.value();
        
        log.info("Consumed chat event: messageId={}, sender={}, recipient={}, bodyLength={}, topic={}, partition={}, offset={}", 
                 event.getMessageId(),
                 event.getMaskedSender(),
                 event.getMaskedRecipient(),
                 event.getBody() != null ? event.getBody().length() : 0,
                 record.topic(),
                 record.partition(),
                 record.offset());
        
        // Phase C2: Log only, no further processing
        // Phase C3: Will add persistence
        // Phase C4: Will add delivery to recipient WebSocket
        
        log.debug("Chat event details: timestamp={}, key={}", 
                  event.getTimestamp(), 
                  record.key());
    }
}
