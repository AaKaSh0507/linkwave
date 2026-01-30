package com.linkwave.app.domain.chat;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA Entity for persisting chat messages.
 * 
 * Phase D: Room-based messaging
 * Messages belong to rooms and are sent by members.
 */
@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_chat_message_room_sent_at", columnList = "room_id, sent_at"),
        @Index(name = "idx_chat_message_sender", columnList = "sender_phone")
})
public class ChatMessageEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id; // UUID string

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoomEntity room;

    @Column(name = "sender_phone", nullable = false, length = 20)
    private String senderPhone;

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

    public ChatRoomEntity getRoom() {
        return room;
    }

    public void setRoom(ChatRoomEntity room) {
        this.room = room;
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
