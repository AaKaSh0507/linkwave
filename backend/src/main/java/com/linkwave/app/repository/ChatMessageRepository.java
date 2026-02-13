package com.linkwave.app.repository;

import com.linkwave.app.domain.chat.ChatMessageEntity;
import com.linkwave.app.domain.chat.ChatRoomEntity;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, String> {

  Page<ChatMessageEntity> findByRoomOrderBySentAtDesc(ChatRoomEntity room, Pageable pageable);

  Page<ChatMessageEntity> findByRoomAndSentAtBeforeOrderBySentAtDesc(
      ChatRoomEntity room, Instant before, Pageable pageable);

  @Modifying
  @Query(
      value =
          "DELETE FROM chat_messages WHERE sent_at < :cutoffTimestamp AND id IN "
              + "(SELECT id FROM chat_messages WHERE sent_at < :cutoffTimestamp LIMIT :batchSize)",
      nativeQuery = true)
  int deleteMessagesOlderThan(
      @Param("cutoffTimestamp") Instant cutoffTimestamp, @Param("batchSize") int batchSize);

  long countBySentAtBefore(Instant timestamp);
}
