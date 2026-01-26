package com.linkwave.app.domain.chat;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA Entity for persisting chat messages.
 * 
 * Phase C4: Database Persistence
 * This entity mirrors the ChatMessage domain model.
 */
@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_chat_sender_recipient_sent_at", columnList = "sender_phone, recipient_phone, sent_at"),
        @Index(name = "idx_chat_recipient_sent_at", columnList = "recipient_phone, sent_at")
})
public class ChatMessageEntity {

    @Id
    @Column(name = "id")
    private String id; // UUID string

    @Column(name = "sender_phone", nullable = false)
    private String senderPhone;

    @Column(name = "recipient_phone", nullable = false)
    private String recipientPhone;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "ttl_days")
    private Integer ttlDays;

    public ChatMessageEntity() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSenderPhone() {
        return senderPhone;
    }

    public void setSenderPhone(String senderPhone) {
        this.senderPhone = senderPhone;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }

    public Integer getTtlDays() {
        return ttlDays;
    }

    public void setTtlDays(Integer ttlDays) {
        this.ttlDays = ttlDays;
    }
}
