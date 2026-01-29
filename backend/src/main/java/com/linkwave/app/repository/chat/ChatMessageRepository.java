package com.linkwave.app.repository.chat;

import com.linkwave.app.domain.chat.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for ChatMessageEntity.
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, String> {

    // Future pagination methods can be added here
    List<ChatMessageEntity> findByRecipientPhoneOrderBySentAtDesc(String recipientPhone);

    List<ChatMessageEntity> findBySenderPhoneOrderBySentAtDesc(String senderPhone);
}
