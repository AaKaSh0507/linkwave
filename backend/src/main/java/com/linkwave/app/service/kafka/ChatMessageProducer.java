package com.linkwave.app.service.kafka;

import com.linkwave.app.domain.chat.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service for publishing chat messages to Kafka.
 * 
 * Phase C3: Refactored to use ChatMessage
 * - Publishes ChatMessage to linkwave.chat.events topic
 * - Partitions by recipient phone number
 */
@Service
public class ChatMessageProducer {

    private static final Logger log = LoggerFactory.getLogger(ChatMessageProducer.class);

    private static final String CHAT_EVENTS_TOPIC = "linkwave.chat.messages.v2";

    private final KafkaTemplate<String, ChatMessage> kafkaTemplate;

    public ChatMessageProducer(KafkaTemplate<String, ChatMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publish chat message to Kafka.
     * Partitions by recipient phone number to ensure ordering.
     */
    public CompletableFuture<SendResult<String, ChatMessage>> publishChatMessage(ChatMessage message) {
        // Use recipient as partition key for ordering
        String partitionKey = message.getRecipientPhoneNumber();

        log.info("Publishing chat message: messageId={}, sender={}, recipient={}, bodyLength={}",
                message.getMessageId(),
                message.getMaskedSender(),
                message.getMaskedRecipient(),
                message.getBody() != null ? message.getBody().length() : 0);

        CompletableFuture<SendResult<String, ChatMessage>> future = kafkaTemplate.send(CHAT_EVENTS_TOPIC, partitionKey,
                message);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                var metadata = result.getRecordMetadata();
                log.info("Chat message published successfully: messageId={}, topic={}, partition={}, offset={}",
                        message.getMessageId(),
                        metadata.topic(),
                        metadata.partition(),
                        metadata.offset());
            } else {
                log.error("Failed to publish chat message: messageId={}, sender={}, recipient={}, error={}",
                        message.getMessageId(),
                        message.getMaskedSender(),
                        message.getMaskedRecipient(),
                        ex.getMessage());
            }
        });

        return future;
    }

    public String getChatEventsTopic() {
        return CHAT_EVENTS_TOPIC;
    }
}
