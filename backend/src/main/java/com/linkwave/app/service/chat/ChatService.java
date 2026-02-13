package com.linkwave.app.service.chat;

import com.linkwave.app.domain.chat.*;
import com.linkwave.app.repository.ChatMemberRepository;
import com.linkwave.app.repository.ChatMessageRepository;
import com.linkwave.app.repository.ChatRoomRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {

  private static final Logger log = LoggerFactory.getLogger(ChatService.class);

  private final ChatRoomRepository roomRepository;
  private final ChatMemberRepository memberRepository;
  private final ChatMessageRepository messageRepository;
  private final KafkaTemplate<String, ChatMessage> kafkaTemplate;
  private final Counter messagesSentSuccess;
  private final Counter messagesSentFailure;
  private final Counter kafkaProducedTotal;
  private final Timer messagePersistenceTimer;
  private final DistributionSummary messageSizeSummary;

  public ChatService(
      ChatRoomRepository roomRepository,
      ChatMemberRepository memberRepository,
      ChatMessageRepository messageRepository,
      KafkaTemplate<String, ChatMessage> kafkaTemplate,
      MeterRegistry meterRegistry) {
    this.roomRepository = roomRepository;
    this.memberRepository = memberRepository;
    this.messageRepository = messageRepository;
    this.kafkaTemplate = kafkaTemplate;

    this.messagesSentSuccess =
        Counter.builder("messages.sent.total").tag("status", "success").register(meterRegistry);
    this.messagesSentFailure =
        Counter.builder("messages.sent.total").tag("status", "failure").register(meterRegistry);
    this.kafkaProducedTotal =
        Counter.builder("kafka.messages.produced.total")
            .tag("topic", "chat.messages")
            .register(meterRegistry);
    this.messagePersistenceTimer =
        Timer.builder("messages.persistence.duration").register(meterRegistry);
    this.messageSizeSummary =
        DistributionSummary.builder("messages.size.bytes").register(meterRegistry);
  }

  @Transactional
  public ChatRoomEntity createDirectRoom(String phoneNumber1, String phoneNumber2) {
    Instant now = Instant.now();

    ChatRoomEntity room =
        new ChatRoomEntity(
            UUID.randomUUID().toString(), ChatRoomEntity.RoomType.DIRECT, null, now, now);

    room = roomRepository.save(room);
    memberRepository.save(new ChatMemberEntity(room, phoneNumber1, now));
    memberRepository.save(new ChatMemberEntity(room, phoneNumber2, now));

    log.info(
        "Created direct room {} for users {} and {}",
        room.getId(),
        maskPhone(phoneNumber1),
        maskPhone(phoneNumber2));

    return room;
  }

  @Transactional
  public ChatRoomEntity createGroupRoom(String name, List<String> memberPhoneNumbers) {
    Instant now = Instant.now();

    ChatRoomEntity room =
        new ChatRoomEntity(
            UUID.randomUUID().toString(), ChatRoomEntity.RoomType.GROUP, name, now, now);

    room = roomRepository.save(room);
    for (String phoneNumber : memberPhoneNumbers) {
      memberRepository.save(new ChatMemberEntity(room, phoneNumber, now));
    }

    log.info("Created group room {} with {} members", room.getId(), memberPhoneNumbers.size());

    return room;
  }

  public ChatMessage sendMessage(String roomId, String senderPhoneNumber, String body) {
    try {
      ChatRoomEntity room =
          roomRepository
              .findById(roomId)
              .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
      if (!memberRepository.existsByRoomAndPhoneNumber(room, senderPhoneNumber)) {
        throw new SecurityException("User is not a member of this room");
      }
      ChatMessage message = ChatMessage.create(roomId, senderPhoneNumber, body);
      if (body != null) {
        messageSizeSummary.record(body.getBytes().length);
      }
      kafkaTemplate.send("chat.messages", roomId, message);
      kafkaProducedTotal.increment();
      messagesSentSuccess.increment();
      log.info("Published message {} to room {}", message.getMessageId(), roomId);

      return message;
    } catch (Exception e) {
      messagesSentFailure.increment();
      throw e;
    }
  }

  @Transactional(readOnly = true)
  public Page<ChatMessageEntity> getRoomMessages(String roomId, Pageable pageable) {
    ChatRoomEntity room =
        roomRepository
            .findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

    return messageRepository.findByRoomOrderBySentAtDesc(room, pageable);
  }

  @Transactional(readOnly = true)
  public List<ChatRoomEntity> getUserRooms(String phoneNumber) {
    return memberRepository.findRoomsByPhoneNumber(phoneNumber);
  }

  @Transactional
  public void persistMessage(ChatMessage message) {
    messagePersistenceTimer.record(
        () -> {
          ChatRoomEntity room =
              roomRepository
                  .findById(message.getRoomId())
                  .orElseThrow(
                      () -> new IllegalArgumentException("Room not found: " + message.getRoomId()));

          ChatMessageEntity entity = new ChatMessageEntity();
          entity.setId(message.getMessageId());
          entity.setRoom(room);
          entity.setSenderPhone(message.getSenderPhoneNumber());
          entity.setBody(message.getBody());
          entity.setSentAt(Instant.ofEpochMilli(message.getSentAt()));
          entity.setTtlDays(message.getTtlDays());

          messageRepository.save(entity);

          log.debug("Persisted message {} to database", message.getMessageId());
        });
  }

  @Transactional(readOnly = true)
  public List<ChatMemberEntity> getRoomMembers(String roomId) {
    ChatRoomEntity room =
        roomRepository
            .findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

    return memberRepository.findByRoom(room);
  }

  private String maskPhone(String phone) {
    if (phone == null || phone.length() < 7) return "***";
    return phone.substring(0, 4) + "***";
  }
}
