package com.linkwave.app.repository;

import com.linkwave.app.domain.chat.ChatMessageEntity;
import com.linkwave.app.domain.chat.ChatRoomEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, String> {

    Page<ChatMessageEntity> findByRoomOrderBySentAtDesc(ChatRoomEntity room, Pageable pageable);
}
