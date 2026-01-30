package com.linkwave.app.repository;

import com.linkwave.app.domain.chat.ChatMemberEntity;
import com.linkwave.app.domain.chat.ChatRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ChatMemberEntity.
 * Phase D: Room membership management
 */
@Repository
public interface ChatMemberRepository extends JpaRepository<ChatMemberEntity, Long> {

    /**
     * Find all members in a room.
     */
    List<ChatMemberEntity> findByRoom(ChatRoomEntity room);

    /**
     * Find all rooms a user is member of.
     */
    @Query("SELECT cm.room FROM ChatMemberEntity cm WHERE cm.phoneNumber = :phoneNumber")
    List<ChatRoomEntity> findRoomsByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    /**
     * Check if user is member of room.
     */
    boolean existsByRoomAndPhoneNumber(ChatRoomEntity room, String phoneNumber);

    /**
     * Find membership record.
     */
    Optional<ChatMemberEntity> findByRoomAndPhoneNumber(ChatRoomEntity room, String phoneNumber);
}
