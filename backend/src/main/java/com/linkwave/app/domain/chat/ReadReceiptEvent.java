package com.linkwave.app.domain.chat;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ReadReceiptEvent {

    @JsonProperty("type")
    private String type = "read.receipt";

    @JsonProperty("roomId")
    private String roomId;

    @JsonProperty("messageId")
    private String messageId;

    @JsonProperty("readerId")
    private String readerId;

    @JsonProperty("timestamp")
    private long timestamp;

    public ReadReceiptEvent() {
    }

    public ReadReceiptEvent(String roomId, String messageId, String readerId, long timestamp) {
        this.roomId = roomId;
        this.messageId = messageId;
        this.readerId = readerId;
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
