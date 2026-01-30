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

        // CRITICAL: Validate that the message belongs to the specified room
        if (!targetMsg.getRoom().getId().equals(roomId)) {
            throw new UnauthorizedException("Message does not belong to specified room");
        }

        // Validate room membership once for the batch
        if (!roomMembershipService.isUserInRoom(readerPhoneNumber, roomId)) {
            throw new UnauthorizedException("Not a room member");
        }

        Instant targetTimestamp = targetMsg.getSentAt();
        Instant maxReadTimestamp = repository.findMaxReadMessageTimestamp(roomId, readerPhoneNumber);

        // Strict ordering: If trying to read something older than what was already
        // read, ignore it.
        // This prevents "filling in gaps" from the past.
        if (maxReadTimestamp != null && !targetTimestamp.isAfter(maxReadTimestamp)) {
            return new ArrayList<>();
        }

        // Find unread messages up to targetTimestamp, BUT STRICTLY AFTER
        // maxReadTimestamp
        List<String> unreadMessageIds = repository.findUnreadMessageIdsUpTo(
                roomId, readerPhoneNumber, targetTimestamp, maxReadTimestamp);

        if (unreadMessageIds.isEmpty()) {
            return new ArrayList<>();
        }

        int MAX_BATCH_SIZE = 50;
        if (unreadMessageIds.size() > MAX_BATCH_SIZE) {
            // If we have more than batch size, we should probably read up to the batch
            // limit
            // But for now, just processing the first 50 found (which are ordered by time
            // ASC)
            // is safe. The user will need to send another read receipt for the rest.
            unreadMessageIds = unreadMessageIds.subList(0, MAX_BATCH_SIZE);
        }

        List<ReadReceiptEntity> newReceipts = new ArrayList<>();
        Instant now = Instant.now();

        for (String msgId : unreadMessageIds) {
            ReadReceiptEntity receipt = new ReadReceiptEntity();
            receipt.setMessageId(msgId);
            receipt.setRoomId(roomId);
            receipt.setReaderPhoneNumber(readerPhoneNumber);
            receipt.setReadAt(now);
            receipt.setCreatedAt(now);
            newReceipts.add(receipt);
        }

        repository.saveAll(newReceipts);

        return newReceipts.stream()
                .map(ReadReceiptResult::newRead)
                .collect(Collectors.toList());
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
