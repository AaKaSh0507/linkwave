package com.linkwave.app.service.kafka;

import com.linkwave.app.domain.chat.ChatMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka consumer for chat messages.
 * 
 * Phase C3: Refactored to use ChatMessage
 * - Receives ChatMessage and logs them
 */
@Service
public class ChatMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(ChatMessageConsumer.class);

    /**
     * Consume chat messages from Kafka.
     */
    @KafkaListener(topics = "linkwave.chat.messages.v2", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "chatMessageKafkaListenerContainerFactory")
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

        log.debug("Chat message details: sentAt={}, key={}",
                message.getSentAt(),
                record.key());
    }
}
