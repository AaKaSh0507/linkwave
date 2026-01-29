package com.linkwave.app.service.kafka;

import com.linkwave.app.domain.chat.ChatMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.linkwave.app.domain.chat.ChatMessageEntity;
import com.linkwave.app.repository.chat.ChatMessageRepository;
import com.linkwave.app.service.chat.ChatFanoutService;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka consumer for chat messages.
 * 
 * Phase C4: Persistence and Fanout
 * - Consumes from Kafka
 * - Persists to Postgres (via JPA)
 * - Delivering to WebSocket (via FanoutService)
 */
@Service
public class ChatMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(ChatMessageConsumer.class);

    private final ChatMessageRepository chatMessageRepository;
    private final ChatFanoutService chatFanoutService;

    public ChatMessageConsumer(ChatMessageRepository chatMessageRepository, ChatFanoutService chatFanoutService) {
        this.chatMessageRepository = chatMessageRepository;
        this.chatFanoutService = chatFanoutService;
    }

    /**
     * Consume chat messages from Kafka.
     * Transactional to ensure DB consistency.
     */
    @KafkaListener(topics = "linkwave.chat.messages.v2", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "chatMessageKafkaListenerContainerFactory")
    @Transactional
    public void consumeChatMessage(ConsumerRecord<String, ChatMessage> record) {
        ChatMessage message = record.value();

        log.info(
                "Consumed chat message: messageId={}, sender={}, recipient={}, bodyLength={}, topic={}, partition={}, offset={}",
                message.getMessageId(),
                message.getMaskedSender(),
                message.getMaskedRecipient(),
                message.getBody() != null ? message.getBody().length() : 0,
                record.topic(),
                record.partition(),
                record.offset());

        // 1. Persist to DB
        ChatMessageEntity entity = mapToEntity(message);
        chatMessageRepository.save(entity);
        log.debug("Persisted message {} to DB", message.getMessageId());

        // 2. Fanout to recipient
        chatFanoutService.deliver(message);
    }

    private ChatMessageEntity mapToEntity(ChatMessage message) {
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setId(message.getMessageId());
        entity.setSenderPhone(message.getSenderPhoneNumber());
        entity.setRecipientPhone(message.getRecipientPhoneNumber());
        entity.setBody(message.getBody());
        entity.setSentAt(java.time.Instant.ofEpochMilli(message.getSentAt()));
        entity.setTtlDays(message.getTtlDays());
        return entity;
    }
}
