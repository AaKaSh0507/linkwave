package com.linkwave.app.domain.chat;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Chat event schema for Kafka messaging.
 * 
 * Phase C2: Initial schema for chat events
 * Phase C3: Will refine schema with additional fields
 * 
 * Schema:
 * {
 *   "messageId": "uuid",
 *   "sender": "+1234567890",
 *   "recipient": "+9876543210",
 *   "body": "message text",
 *   "timestamp": 1234567890123
 * }
 */
public class ChatEvent {
    
    @JsonProperty("messageId")
    private String messageId;
    
    @JsonProperty("sender")
    private String sender;
    
    @JsonProperty("recipient")
    private String recipient;
    
    @JsonProperty("body")
    private String body;
    
    @JsonProperty("timestamp")
    private long timestamp;
    
    public ChatEvent() {
    }
    
    public ChatEvent(String messageId, String sender, String recipient, String body, long timestamp) {
        this.messageId = messageId;
        this.sender = sender;
        this.recipient = recipient;
        this.body = body;
        this.timestamp = timestamp;
    }
    
    /**
     * Create ChatEvent with server-generated messageId and timestamp.
     */
    public static ChatEvent create(String sender, String recipient, String body) {
        return new ChatEvent(
            UUID.randomUUID().toString(),
            sender,
            recipient,
            body,
            Instant.now().toEpochMilli()
        );
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public String getSender() {
        return sender;
    }
    
    public void setSender(String sender) {
        this.sender = sender;
    }
    
    public String getRecipient() {
        return recipient;
    }
    
    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }
    
    public String getBody() {
        return body;
    }
    
    public void setBody(String body) {
        this.body = body;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * Mask phone number for logging (show first 4 chars and last 2).
     */
    public static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 7) {
            return "***";
        }
        return phoneNumber.substring(0, 4) + "***" + phoneNumber.substring(phoneNumber.length() - 2);
    }
    
    /**
     * Get masked sender for safe logging.
     */
    public String getMaskedSender() {
        return maskPhoneNumber(sender);
    }
    
    /**
     * Get masked recipient for safe logging.
     */
    public String getMaskedRecipient() {
        return maskPhoneNumber(recipient);
    }
    
    @Override
    public String toString() {
        return "ChatEvent{" +
                "messageId='" + messageId + '\'' +
                ", sender='" + getMaskedSender() + '\'' +
                ", recipient='" + getMaskedRecipient() + '\'' +
                ", bodyLength=" + (body != null ? body.length() : 0) +
                ", timestamp=" + timestamp +
                '}';
    }
}
