package com.linkwave.app.service.chat;

import com.linkwave.app.domain.chat.ChatMessageEntity;
import com.linkwave.app.domain.chat.ChatRoomEntity;
import com.linkwave.app.domain.chat.MessageDTO;
import com.linkwave.app.domain.chat.PaginatedMessagesResponse;
import com.linkwave.app.repository.ChatMemberRepository;
import com.linkwave.app.repository.ChatMessageRepository;
import com.linkwave.app.repository.ChatRoomRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageHistoryService {

  private static final int MAX_MESSAGES_PER_REQUEST = 50;

  private final ChatMessageRepository messageRepository;
  private final ChatMemberRepository memberRepository;
  private final ChatRoomRepository roomRepository;

  public MessageHistoryService(
      ChatMessageRepository messageRepository,
      ChatMemberRepository memberRepository,
      ChatRoomRepository roomRepository) {
    this.messageRepository = messageRepository;
    this.memberRepository = memberRepository;
    this.roomRepository = roomRepository;
  }

  @Transactional(readOnly = true)
  public List<MessageDTO> getDirectMessageHistory(String currentUserPhone, String otherUserPhone) {
    ChatRoomEntity room =
        findDirectRoomBetweenUsers(currentUserPhone, otherUserPhone)
            .orElseThrow(
                () -> new IllegalArgumentException("No conversation found with this user"));

    Page<ChatMessageEntity> messages =
        messageRepository.findByRoomOrderBySentAtDesc(
            room, PageRequest.of(0, MAX_MESSAGES_PER_REQUEST));

    return messages.getContent().stream().map(MessageDTO::fromEntity).toList();
  }

  @Transactional(readOnly = true)
  public PaginatedMessagesResponse getDirectMessageHistoryPaginated(
      String currentUserPhone, String otherUserPhone, Instant before) {

    ChatRoomEntity room =
        findDirectRoomBetweenUsers(currentUserPhone, otherUserPhone)
            .orElseThrow(
                () -> new IllegalArgumentException("No conversation found with this user"));

    PageRequest pageRequest = PageRequest.of(0, MAX_MESSAGES_PER_REQUEST + 1);
    Page<ChatMessageEntity> messagePage;

    if (before != null) {
      messagePage =
          messageRepository.findByRoomAndSentAtBeforeOrderBySentAtDesc(room, before, pageRequest);
    } else {
      messagePage = messageRepository.findByRoomOrderBySentAtDesc(room, pageRequest);
    }

    List<ChatMessageEntity> entities = messagePage.getContent();
    boolean hasMore = entities.size() > MAX_MESSAGES_PER_REQUEST;

    List<MessageDTO> messages =
        entities.stream().limit(MAX_MESSAGES_PER_REQUEST).map(MessageDTO::fromEntity).toList();

    return PaginatedMessagesResponse.of(messages, hasMore);
  }

  private Optional<ChatRoomEntity> findDirectRoomBetweenUsers(String userA, String userB) {
    List<ChatRoomEntity> userARooms = memberRepository.findRoomsByPhoneNumber(userA);

    return userARooms.stream()
        .filter(room -> room.getRoomType() == ChatRoomEntity.RoomType.DIRECT)
        .filter(room -> memberRepository.existsByRoomAndPhoneNumber(room, userB))
        .findFirst();
  }
}
