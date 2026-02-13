package com.linkwave.app.util;

import com.linkwave.app.domain.chat.ChatMessageEntity;
import com.linkwave.app.domain.chat.ChatRoomEntity;
import java.time.Instant;
import java.util.UUID;

public final class TestDataBuilder {

  private TestDataBuilder() {}

  public static UserEntity createUser(String phone) {
    return UserBuilder.user().withPhone(phone).build();
  }

  public static String createSession(Long userId) {
    return SessionBuilder.session().withUserId(userId).build();
  }

  public static ChatMessageEntity createMessage(Long senderId, Long recipientId, String body) {
    return MessageBuilder.message()
        .withSenderId(senderId)
        .withRecipientId(recipientId)
        .withBody(body)
        .build();
  }

  public static ChatRoomEntity createRoom(Long user1Id, Long user2Id) {
    return RoomBuilder.room().withUser1Id(user1Id).withUser2Id(user2Id).build();
  }

  public static final class UserEntity {
    private final Long id;
    private final String phone;

    public UserEntity(Long id, String phone) {
      this.id = id;
      this.phone = phone;
    }

    public Long getId() {
      return id;
    }

    public String getPhone() {
      return phone;
    }
  }

  public static final class UserBuilder {
    private Long id = Math.abs(UUID.randomUUID().getMostSignificantBits());
    private String phone = "+14155550000";

    private UserBuilder() {}

    public static UserBuilder user() {
      return new UserBuilder();
    }

    public UserBuilder withId(Long id) {
      this.id = id;
      return this;
    }

    public UserBuilder withPhone(String phone) {
      this.phone = phone;
      return this;
    }

    public UserEntity build() {
      return new UserEntity(id, phone);
    }
  }

  public static final class SessionBuilder {
    private Long userId = 1L;

    private SessionBuilder() {}

    public static SessionBuilder session() {
      return new SessionBuilder();
    }

    public SessionBuilder withUserId(Long userId) {
      this.userId = userId;
      return this;
    }

    public String build() {
      return "session-" + userId + "-" + UUID.randomUUID();
    }
  }

  public static final class MessageBuilder {
    private Long senderId = 1L;
    private Long recipientId = 2L;
    private String body = "test-message";
    private Instant sentAt = Instant.now();

    private MessageBuilder() {}

    public static MessageBuilder message() {
      return new MessageBuilder();
    }

    public MessageBuilder withSenderId(Long senderId) {
      this.senderId = senderId;
      return this;
    }

    public MessageBuilder withRecipientId(Long recipientId) {
      this.recipientId = recipientId;
      return this;
    }

    public MessageBuilder withBody(String body) {
      this.body = body;
      return this;
    }

    public MessageBuilder withSentAt(Instant sentAt) {
      this.sentAt = sentAt;
      return this;
    }

    public ChatMessageEntity build() {
      ChatMessageEntity entity = new ChatMessageEntity();
      entity.setId(UUID.randomUUID().toString());
      entity.setSenderPhone("+1" + senderId);
      entity.setBody(body + "::to::" + recipientId);
      entity.setSentAt(sentAt);
      return entity;
    }
  }

  public static final class RoomBuilder {
    private Long user1Id = 1L;
    private Long user2Id = 2L;

    private RoomBuilder() {}

    public static RoomBuilder room() {
      return new RoomBuilder();
    }

    public RoomBuilder withUser1Id(Long user1Id) {
      this.user1Id = user1Id;
      return this;
    }

    public RoomBuilder withUser2Id(Long user2Id) {
      this.user2Id = user2Id;
      return this;
    }

    public ChatRoomEntity build() {
      Instant now = Instant.now();
      ChatRoomEntity room = new ChatRoomEntity();
      room.setId(UUID.randomUUID().toString());
      room.setRoomType(ChatRoomEntity.RoomType.DIRECT);
      room.setName("room-" + user1Id + "-" + user2Id);
      room.setCreatedAt(now);
      room.setUpdatedAt(now);
      return room;
    }
  }
}
