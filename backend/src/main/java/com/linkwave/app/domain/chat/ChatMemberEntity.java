package com.linkwave.app.domain.chat;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA Entity for chat room members.
 * 
 * Phase D: Room membership tracking
 * Maps users to rooms with metadata like join time and last read.
 */
@Entity
@Table(name = "chat_members", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "phone_number"}),
       indexes = {
        @Index(name = "idx_chat_member_phone", columnList = "phone_number"),
        @Index(name = "idx_chat_member_room", columnList = "room_id")
})
public class ChatMemberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoomEntity room;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "last_read_at")
    private Instant lastReadAt;

    public ChatMemberEntity() {
    }

    public ChatMemberEntity(ChatRoomEntity room, String phoneNumber, Instant joinedAt) {
        this.room = room;
        this.phoneNumber = phoneNumber;
        this.joinedAt = joinedAt;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ChatRoomEntity getRoom() {
        return room;
    }

    public void setRoom(ChatRoomEntity room) {
        this.room = room;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Instant getLastReadAt() {
        return lastReadAt;
    }

    public void setLastReadAt(Instant lastReadAt) {
        this.lastReadAt = lastReadAt;
    }
}
