package com.linkwave.app.service.readreceipt;

import com.linkwave.app.domain.chat.ReadReceiptEntity;
import com.linkwave.app.repository.ReadReceiptRepository;
import com.linkwave.app.service.room.RoomMembershipService;
import com.linkwave.app.repository.ChatMessageRepository;
import com.linkwave.app.domain.chat.ChatMessageEntity;
import com.linkwave.app.exception.NotFoundException;
import com.linkwave.app.exception.UnauthorizedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReadReceiptService {

    private final ReadReceiptRepository repository;
    private final RoomMembershipService roomMembershipService;
    private final ChatMessageRepository messageRepository;

    public ReadReceiptService(
            ReadReceiptRepository repository,
            RoomMembershipService roomMembershipService,
            ChatMessageRepository messageRepository) {
        this.repository = repository;
        this.roomMembershipService = roomMembershipService;
        this.messageRepository = messageRepository;
    }

    public static class ReadReceiptResult {
        private final boolean newRead;
        private final ReadReceiptEntity receipt;

        public ReadReceiptResult(boolean newRead, ReadReceiptEntity receipt) {
            this.newRead = newRead;
            this.receipt = receipt;
        }

        public static ReadReceiptResult alreadyRead() {
            return new ReadReceiptResult(false, null);
        }

        public static ReadReceiptResult newRead(ReadReceiptEntity receipt) {
            return new ReadReceiptResult(true, receipt);
        }

        public boolean isNewRead() {
            return newRead;
        }

        public ReadReceiptEntity getReceipt() {
            return receipt;
        }
    }

    @Transactional
    public ReadReceiptResult markMessageRead(
            String messageId,
            String roomId,
            String readerPhoneNumber) {
        if (repository.existsByMessageIdAndReaderPhoneNumber(messageId, readerPhoneNumber)) {
            return ReadReceiptResult.alreadyRead();
        }

        if (!roomMembershipService.isUserInRoom(readerPhoneNumber, roomId)) {
            throw new UnauthorizedException("Not a room member");
        }

        ReadReceiptEntity receipt = new ReadReceiptEntity();
        receipt.setMessageId(messageId);
        receipt.setRoomId(roomId);
        receipt.setReaderPhoneNumber(readerPhoneNumber);
        receipt.setReadAt(Instant.now());
        receipt.setCreatedAt(Instant.now());

        repository.save(receipt);

        return ReadReceiptResult.newRead(receipt);
    }

    @Transactional
    public List<ReadReceiptResult> markReadUpTo(
            String roomId,
            String messageId,
            String readerPhoneNumber) {

        ChatMessageEntity targetMsg = messageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Message not found"));
        Instant targetTimestamp = targetMsg.getSentAt();

        Instant maxReadTimestamp = repository.findMaxReadMessageTimestamp(roomId, readerPhoneNumber);
        if (maxReadTimestamp != null && targetTimestamp.isBefore(maxReadTimestamp)) {
            return new ArrayList<>();
        }

        List<String> unreadMessageIds = repository.findUnreadMessageIdsUpTo(
                roomId, readerPhoneNumber, targetTimestamp);

        int MAX_BATCH_SIZE = 50;
        if (unreadMessageIds.size() > MAX_BATCH_SIZE) {
            unreadMessageIds = unreadMessageIds.subList(0, MAX_BATCH_SIZE);
        }

        List<ReadReceiptResult> results = new ArrayList<>();
        for (String msgId : unreadMessageIds) {
            results.add(markMessageRead(msgId, roomId, readerPhoneNumber));
        }

        return results;
    }

    public List<String> getMessageReaders(String messageId) {
        return repository.findByMessageId(messageId)
                .stream()
                .map(ReadReceiptEntity::getReaderPhoneNumber)
                .collect(Collectors.toList());
    }

    
    public long getReadCount(String messageId) {
        return repository.countByMessageId(messageId);
    }
}
