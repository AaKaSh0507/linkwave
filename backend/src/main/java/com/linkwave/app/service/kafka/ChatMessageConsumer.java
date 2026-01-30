package com.linkwave.app.service.kafka;

import com.linkwave.app.domain.chat.ChatMessage;
import com.linkwave.app.service.chat.ChatService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka consumer for chat messages (Phase D).
 * 
 * Responsibilities:
 * 1. Consume messages from "chat.messages" topic
 * 2. Persist to database via ChatService
 * 3. Broadcast to room subscribers via STOMP
 */
@Service
public class ChatMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(ChatMessageConsumer.class);

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatMessageConsumer(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(
        topics = "chat.messages",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "chatMessageKafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeChatMessage(ConsumerRecord<String, ChatMessage> record) {
        ChatMessage message = record.value();

        log.info(
            "Consumed chat message: messageId={}, roomId={}, sender={}, bodyLength={}, partition={}, offset={}",
            message.getMessageId(),
            message.getRoomId(),
            message.getMaskedSender(),
            message.getBody() != null ? message.getBody().length() : 0,
            record.partition(),
            record.offset()
        );

        try {
            // 1. Persist to database
            chatService.persistMessage(message);
            log.debug("Persisted message {} to DB", message.getMessageId());

            // 2. Broadcast to room subscribers via STOMP
            messagingTemplate.convertAndSend("/topic/room." + message.getRoomId(), message);
            log.debug("Broadcasted message {} to /topic/room.{}", message.getMessageId(), message.getRoomId());
            
        } catch (Exception e) {
            log.error("Failed to process message {}: {}", message.getMessageId(), e.getMessage(), e);
            throw e; // Re-throw to trigger Kafka retry
        }
    }
}
