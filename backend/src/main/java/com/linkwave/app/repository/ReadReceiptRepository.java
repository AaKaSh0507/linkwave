package com.linkwave.app.repository;

import com.linkwave.app.domain.chat.ReadReceiptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ReadReceiptRepository extends JpaRepository<ReadReceiptEntity, Long> {

    
    boolean existsByMessageIdAndReaderPhoneNumber(String messageId, String readerPhoneNumber);

    
    List<ReadReceiptEntity> findByMessageId(String messageId);

    
    @Query("SELECT COUNT(r) FROM ReadReceiptEntity r WHERE r.messageId = :messageId")
    long countByMessageId(@Param("messageId") String messageId);

    
    List<ReadReceiptEntity> findByRoomIdAndReaderPhoneNumber(
            String roomId, String readerPhoneNumber);

    
    
    
    
    
    
    
    
    
    
    
    

    
    
    @Query("SELECT m.id FROM ChatMessageEntity m " +
            "WHERE m.room.id = :roomId " +
            "AND m.sentAt <= :targetTimestamp " +
            "AND m.id NOT IN (" +
            "    SELECT r.messageId FROM ReadReceiptEntity r " +
            "    WHERE r.roomId = :roomId " +
            "    AND r.readerPhoneNumber = :readerPhoneNumber" +
            ") " +
            "ORDER BY m.sentAt ASC")
    List<String> findUnreadMessageIdsUpTo(
            @Param("roomId") String roomId,
            @Param("readerPhoneNumber") String readerPhoneNumber,
            @Param("targetTimestamp") Instant targetTimestamp);

    
    
    
    @Query("SELECT MAX(m.sentAt) FROM ReadReceiptEntity r, ChatMessageEntity m " +
            "WHERE r.messageId = m.id " +
            "AND r.roomId = :roomId " +
            "AND r.readerPhoneNumber = :readerPhoneNumber")
    Instant findMaxReadMessageTimestamp(
            @Param("roomId") String roomId,
            @Param("readerPhoneNumber") String readerPhoneNumber);
}
