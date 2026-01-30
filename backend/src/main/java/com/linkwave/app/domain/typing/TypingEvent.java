package com.linkwave.app.domain.typing;

import com.fasterxml.jackson.annotation.JsonProperty;


public class TypingEvent {

    @JsonProperty("type")
    private String type = "typing.event";

    @JsonProperty("action")
    private TypingAction action;

    @JsonProperty("senderId")
    private String senderId;

    @JsonProperty("roomId")
    private String roomId;

    @JsonProperty("timestamp")
    private long timestamp;

    public enum TypingAction {
        @JsonProperty("start")
        START,

        @JsonProperty("stop")
        STOP
    }

    public TypingEvent() {
    }

    public TypingEvent(String senderId, String roomId, TypingAction action) {
        this.senderId = senderId;
        this.roomId = roomId;
        this.action = action;
        this.timestamp = System.currentTimeMillis();
    }

    public TypingEvent(String senderId, String roomId, TypingAction action, long timestamp) {
        this.senderId = senderId;
        this.roomId = roomId;
        this.action = action;
        this.timestamp = timestamp;
    }

    

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public TypingAction getAction() {
        return action;
    }

    public void setAction(TypingAction action) {
        this.action = action;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "TypingEvent{" +
                "action=" + action +
                ", senderId='" + maskPhoneNumber(senderId) + '\'' +
                ", roomId='" + roomId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    private static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 7) {
            return "***";
        }
        return phoneNumber.substring(0, 4) + "***" + phoneNumber.substring(phoneNumber.length() - 2);
    }
}
