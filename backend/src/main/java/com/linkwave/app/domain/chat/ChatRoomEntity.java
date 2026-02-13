package com.linkwave.app.domain.chat;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
    name = "chat_rooms",
    indexes = {
      @Index(name = "idx_chat_room_type", columnList = "room_type"),
      @Index(name = "idx_chat_room_created_at", columnList = "created_at")
    })
public class ChatRoomEntity {

  @Id
  @Column(name = "id", length = 36)
  private String id;

  @Column(name = "room_type", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  private RoomType roomType;

  @Column(name = "name", length = 255)
  private String name;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<ChatMemberEntity> members = new HashSet<>();

  public enum RoomType {
    DIRECT,
    GROUP
  }

  public ChatRoomEntity() {}

  public ChatRoomEntity(
      String id, RoomType roomType, String name, Instant createdAt, Instant updatedAt) {
    this.id = id;
    this.roomType = roomType;
    this.name = name;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public RoomType getRoomType() {
    return roomType;
  }

  public void setRoomType(RoomType roomType) {
    this.roomType = roomType;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public Set<ChatMemberEntity> getMembers() {
    return members;
  }

  public void setMembers(Set<ChatMemberEntity> members) {
    this.members = members;
  }
}
