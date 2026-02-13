package com.linkwave.app.service.readreceipt;

import com.linkwave.app.domain.chat.ChatMessageEntity;
import com.linkwave.app.domain.chat.ReadReceiptEntity;
import com.linkwave.app.exception.NotFoundException;
import com.linkwave.app.exception.UnauthorizedException;
import com.linkwave.app.repository.ChatMessageRepository;
import com.linkwave.app.repository.ReadReceiptRepository;
import com.linkwave.app.service.room.RoomMembershipService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReadReceiptService {

  private static final Logger log = LoggerFactory.getLogger(ReadReceiptService.class);

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
      String messageId, String roomId, String readerPhoneNumber) {
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
      String roomId, String messageId, String readerPhoneNumber) {

    ChatMessageEntity targetMsg =
        messageRepository
            .findById(messageId)
            .orElseThrow(() -> new NotFoundException("Message not found"));

    if (!targetMsg.getRoom().getId().equals(roomId)) {
      throw new UnauthorizedException("Message does not belong to specified room");
    }

    if (!roomMembershipService.isUserInRoom(readerPhoneNumber, roomId)) {
      throw new UnauthorizedException("Not a room member");
    }

    Instant targetTimestamp = targetMsg.getSentAt();
    Instant maxReadTimestamp = repository.findMaxReadMessageTimestamp(roomId, readerPhoneNumber);

    if (maxReadTimestamp != null && !targetTimestamp.isAfter(maxReadTimestamp)) {
      return new ArrayList<>();
    }

    List<String> unreadMessageIds =
        repository.findUnreadMessageIdsUpTo(
            roomId, readerPhoneNumber, targetTimestamp, maxReadTimestamp);

    if (unreadMessageIds.isEmpty()) {
      return new ArrayList<>();
    }

    int MAX_BATCH_SIZE = 50;
    if (unreadMessageIds.size() > MAX_BATCH_SIZE) {
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
    log.info(
        "Read receipts created: room_id={} reader={} count={}",
        roomId,
        maskPhone(readerPhoneNumber),
        newReceipts.size());

    return newReceipts.stream().map(ReadReceiptResult::newRead).collect(Collectors.toList());
  }

  public List<String> getMessageReaders(String messageId) {
    return repository.findByMessageId(messageId).stream()
        .map(ReadReceiptEntity::getReaderPhoneNumber)
        .collect(Collectors.toList());
  }

  public long getReadCount(String messageId) {
    return repository.countByMessageId(messageId);
  }

  private String maskPhone(String phone) {
    if (phone == null || phone.length() < 4) {
      return "***";
    }
    return "***" + phone.substring(phone.length() - 4);
  }
}
