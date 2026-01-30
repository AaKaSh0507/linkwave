package com.linkwave.app.repository;

import com.linkwave.app.domain.chat.ChatRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for ChatRoomEntity.
 * Phase D: Room-based messaging
 */
@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoomEntity, String> {
}
