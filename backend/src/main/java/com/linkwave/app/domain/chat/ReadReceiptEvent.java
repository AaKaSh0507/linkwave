package com.linkwave.app.domain.chat;

public class ReadReceiptEvent {
    private String roomId;
    private String messageId;
    private String readerId;
    private long timestamp;

    public ReadReceiptEvent() {
    }

    public ReadReceiptEvent(String roomId, String messageId, String readerId, long timestamp) {
        this.roomId = roomId;
        this.messageId = messageId;
        this.readerId = readerId;
        this.timestamp = timestamp;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getReaderId() {
        return readerId;
    }

    public void setReaderId(String readerId) {
        this.readerId = readerId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
