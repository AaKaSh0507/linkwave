package com.linkwave.app.service.retention;

import com.linkwave.app.config.retention.RetentionProperties;
import com.linkwave.app.repository.ChatMessageRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class RetentionBatchDeleter {

    private final ChatMessageRepository chatMessageRepository;
    private final RetentionProperties retentionProperties;

    public RetentionBatchDeleter(ChatMessageRepository chatMessageRepository, RetentionProperties retentionProperties) {
        this.chatMessageRepository = chatMessageRepository;
        this.retentionProperties = retentionProperties;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int deleteBatch(Instant cutoffTimestamp) {
        return chatMessageRepository.deleteMessagesOlderThan(cutoffTimestamp, retentionProperties.getBatchSize());
    }
}

