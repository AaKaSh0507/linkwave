package com.linkwave.app.domain.chat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.UUID;

public class ChatMessage implements Serializable {

  @JsonProperty("messageId")
  private String messageId;

  @JsonProperty("roomId")
  private String roomId;

  @JsonProperty("senderPhoneNumber")
  private String senderPhoneNumber;

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

  public ChatMessage() {}

  public ChatMessage(
      String messageId,
      String roomId,
      String senderPhoneNumber,
      String body,
      long sentAt,
      Integer ttlDays) {
    this.messageId = messageId;
    this.roomId = roomId;
    this.senderPhoneNumber = senderPhoneNumber;
    this.body = body;
    this.sentAt = sentAt;
    this.ttlDays = ttlDays;
  }

  public static ChatMessage create(String roomId, String senderPhoneNumber, String body) {
    return new ChatMessage(
        UUID.randomUUID().toString(),
        roomId,
        senderPhoneNumber,
        body,
        System.currentTimeMillis(),
        7);
  }

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

  public String getRoomId() {
    return roomId;
  }

  public void setRoomId(String roomId) {
    this.roomId = roomId;
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

  @JsonIgnore
  public String getMaskedSender() {
    return maskPhoneNumber(senderPhoneNumber);
  }

  private static String maskPhoneNumber(String phoneNumber) {
    if (phoneNumber == null || phoneNumber.length() < 7) {
      return "***";
    }
    return phoneNumber.substring(0, 4) + "***" + phoneNumber.substring(phoneNumber.length() - 2);
  }

  @Override
  public String toString() {
    return "ChatMessage{"
        + "messageId='"
        + messageId
        + '\''
        + ", roomId='"
        + roomId
        + '\''
        + ", sender='"
        + getMaskedSender()
        + '\''
        + ", bodyLength="
        + (body != null ? body.length() : 0)
        + ", sentAt="
        + sentAt
        + '}';
  }
}
