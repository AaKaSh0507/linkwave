package com.linkwave.app.domain.chat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.UUID;

/**
 * Canonical ChatMessage schema for Linkwave.
 * Shared across WebSocket, Kafka, and Database.
 * 
 * Phase C3: Schema Definition
 * 
 * Invariants:
 * - messageId is immutable and server-generated
 * - sender != recipient
 * - body not empty
 * - sentAt <= deliveredAt <= readAt
 */
public class ChatMessage implements Serializable {

    @JsonProperty("messageId")
    private String messageId;

    @JsonProperty("senderPhoneNumber")
    private String senderPhoneNumber;

    @JsonProperty("recipientPhoneNumber")
    private String recipientPhoneNumber;

    @JsonProperty("body")
    private String body;

    @JsonProperty("sentAt")
    private long sentAt;

    @JsonProperty("deliveredAt")
    private Long deliveredAt;

    @JsonProperty("readAt")
    private Long readAt;

    @JsonProperty("ttlDays")
    private Integer ttlDays;

    public ChatMessage() {
    }

    public ChatMessage(String messageId, String senderPhoneNumber, String recipientPhoneNumber,
            String body, long sentAt, Integer ttlDays) {
        this.messageId = messageId;
        this.senderPhoneNumber = senderPhoneNumber;
        this.recipientPhoneNumber = recipientPhoneNumber;
        this.body = body;
        this.sentAt = sentAt;
        this.ttlDays = ttlDays;
    }

    /**
     * Create a new ChatMessage with generated ID and timestamp.
     */
    public static ChatMessage create(String senderPhoneNumber, String recipientPhoneNumber, String body) {
        return new ChatMessage(
                UUID.randomUUID().toString(),
                senderPhoneNumber,
                recipientPhoneNumber,
                body,
                System.currentTimeMillis(),
                7 // Default retention 7 days
        );
    }

    // Getters and Setters

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderPhoneNumber() {
        return senderPhoneNumber;
    }

    public void setSenderPhoneNumber(String senderPhoneNumber) {
        this.senderPhoneNumber = senderPhoneNumber;
    }

    public String getRecipientPhoneNumber() {
        return recipientPhoneNumber;
    }

    public void setRecipientPhoneNumber(String recipientPhoneNumber) {
        this.recipientPhoneNumber = recipientPhoneNumber;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public long getSentAt() {
        return sentAt;
    }

    public void setSentAt(long sentAt) {
        this.sentAt = sentAt;
    }

    public Long getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Long deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public Long getReadAt() {
        return readAt;
    }

    public void setReadAt(Long readAt) {
        this.readAt = readAt;
    }

    public Integer getTtlDays() {
        return ttlDays;
    }

    public void setTtlDays(Integer ttlDays) {
        this.ttlDays = ttlDays;
    }

    /**
     * Helpers for logging
     */
    @JsonIgnore
    public String getMaskedSender() {
        return maskPhoneNumber(senderPhoneNumber);
    }

    @JsonIgnore
    public String getMaskedRecipient() {
        return maskPhoneNumber(recipientPhoneNumber);
    }

    private static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 7) {
            return "***";
        }
        return phoneNumber.substring(0, 4) + "***" + phoneNumber.substring(phoneNumber.length() - 2);
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "messageId='" + messageId + '\'' +
                ", sender='" + getMaskedSender() + '\'' +
                ", recipient='" + getMaskedRecipient() + '\'' +
                ", bodyLength=" + (body != null ? body.length() : 0) +
                ", sentAt=" + sentAt +
                '}';
    }
}
