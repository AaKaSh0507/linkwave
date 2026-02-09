package com.linkwave.app.domain.chat;

import java.time.Instant;

public class MessageDTO {
    private String id;
    private String roomId;
    private String senderPhone;
    private String body;
    private Instant sentAt;
    private Instant readAt;

    public MessageDTO() {
    }

    public MessageDTO(String id, String roomId, String senderPhone, String body, Instant sentAt, Instant readAt) {
        this.id = id;
        this.roomId = roomId;
        this.senderPhone = senderPhone;
        this.body = body;
        this.sentAt = sentAt;
        this.readAt = readAt;
    }

    public static MessageDTO fromEntity(ChatMessageEntity entity) {
        return new MessageDTO(
                entity.getId(),
                entity.getRoom().getId(),
                entity.getSenderPhone(),
                entity.getBody(),
                entity.getSentAt(),
                entity.getReadAt());
    }

    public String getId() {
        return id;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getSenderPhone() {
        return senderPhone;
    }

    public String getBody() {
        return body;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getReadAt() {
        return readAt;
    }
}
