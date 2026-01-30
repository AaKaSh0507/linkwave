package com.linkwave.app.service.readreceipt;

import com.linkwave.app.domain.chat.ChatMessageEntity;
import com.linkwave.app.domain.chat.ReadReceiptEntity;
import com.linkwave.app.repository.ChatMessageRepository;
import com.linkwave.app.repository.ReadReceiptRepository;
import com.linkwave.app.service.readreceipt.ReadReceiptService.ReadReceiptResult;
import com.linkwave.app.service.room.RoomMembershipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadReceiptHardeningTest {

    @Mock
    private ReadReceiptRepository repository;
    @Mock
    private RoomMembershipService roomMembershipService;
    @Mock
    private ChatMessageRepository messageRepository;

    private ReadReceiptService service;

    private static final String ROOM_ID = "room-harden";
    private static final String READER_PHONE = "+15559990000";

    @BeforeEach
    void setUp() {
        service = new ReadReceiptService(repository, roomMembershipService, messageRepository);
    }

    @Test
    void markReadUpTo_whenNewerMessageAlreadyRead_shouldIgnoreHelper() {

        String oldMsgId = "msg-old";
        Instant oldTime = Instant.now().minusSeconds(60);

        ChatMessageEntity oldMsg = new ChatMessageEntity();
        oldMsg.setId(oldMsgId);
        oldMsg.setSentAt(oldTime);

        // Mock room relationship
        com.linkwave.app.domain.chat.ChatRoomEntity room = new com.linkwave.app.domain.chat.ChatRoomEntity();
        room.setId(ROOM_ID);
        oldMsg.setRoom(room);

        when(messageRepository.findById(oldMsgId)).thenReturn(Optional.of(oldMsg));

        Instant newerTime = Instant.now();
        when(repository.findMaxReadMessageTimestamp(ROOM_ID, READER_PHONE)).thenReturn(newerTime);
        when(roomMembershipService.isUserInRoom(READER_PHONE, ROOM_ID)).thenReturn(true);

        List<ReadReceiptResult> results = service.markReadUpTo(ROOM_ID, oldMsgId, READER_PHONE);

        assertThat(results).isEmpty();

        verify(repository, never()).findUnreadMessageIdsUpTo(any(), any(), any(), any());
    }

    @Test
    void markReadUpTo_shouldRespectBatchLimit() {

        String targetMsgId = "msg-limit";
        Instant targetTime = Instant.now();
        ChatMessageEntity targetMsg = new ChatMessageEntity();
        targetMsg.setId(targetMsgId);
        targetMsg.setSentAt(targetTime);

        // Mock room relationship
        com.linkwave.app.domain.chat.ChatRoomEntity room = new com.linkwave.app.domain.chat.ChatRoomEntity();
        room.setId(ROOM_ID);
        targetMsg.setRoom(room);

        when(messageRepository.findById(targetMsgId)).thenReturn(Optional.of(targetMsg));
        when(repository.findMaxReadMessageTimestamp(ROOM_ID, READER_PHONE)).thenReturn(null);

        List<String> manyMessages = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            manyMessages.add("msg-" + i);
        }
        when(repository.findUnreadMessageIdsUpTo(ROOM_ID, READER_PHONE, targetTime, null))
                .thenReturn(manyMessages);

        when(roomMembershipService.isUserInRoom(READER_PHONE, ROOM_ID)).thenReturn(true);

        List<ReadReceiptResult> results = service.markReadUpTo(ROOM_ID, targetMsgId, READER_PHONE);

        assertThat(results).hasSize(50);
        verify(repository).saveAll(anyList());
    }

    @Test
    void markReadUpTo_whenMessageFromDifferentRoom_shouldReject() {
        // Arrange - message belongs to room-other, but trying to mark read in
        // room-harden
        String targetMsgId = "msg-wrong-room";
        Instant targetTime = Instant.now();

        ChatMessageEntity targetMsg = new ChatMessageEntity();
        targetMsg.setId(targetMsgId);
        targetMsg.setSentAt(targetTime);

        // Create a room entity for the message (different from ROOM_ID)
        com.linkwave.app.domain.chat.ChatRoomEntity wrongRoom = new com.linkwave.app.domain.chat.ChatRoomEntity();
        wrongRoom.setId("room-other");
        targetMsg.setRoom(wrongRoom);

        when(messageRepository.findById(targetMsgId)).thenReturn(Optional.of(targetMsg));

        // Act & Assert
        org.junit.jupiter.api.Assertions.assertThrows(
                com.linkwave.app.exception.UnauthorizedException.class,
                () -> service.markReadUpTo(ROOM_ID, targetMsgId, READER_PHONE),
                "Should reject message from different room");

        // Verify no reads were persisted
        verify(repository, never()).save(any(ReadReceiptEntity.class));
    }

    @Test
    void markReadUpTo_whenCalledTwice_shouldReturnEmptySecondTime() {
        String targetMsgId = "msg-duplicate";
        Instant targetTime = Instant.now();
        ChatMessageEntity targetMsg = new ChatMessageEntity();
        targetMsg.setId(targetMsgId);
        targetMsg.setSentAt(targetTime);

        // Mock room relationship
        com.linkwave.app.domain.chat.ChatRoomEntity room = new com.linkwave.app.domain.chat.ChatRoomEntity();
        room.setId(ROOM_ID);
        targetMsg.setRoom(room);

        when(messageRepository.findById(targetMsgId)).thenReturn(Optional.of(targetMsg));
        when(roomMembershipService.isUserInRoom(READER_PHONE, ROOM_ID)).thenReturn(true);

        // First call: maxReadTimestamp is null (nothing read yet)
        when(repository.findMaxReadMessageTimestamp(ROOM_ID, READER_PHONE)).thenReturn(null);
        // And there is one unread message
        when(repository.findUnreadMessageIdsUpTo(ROOM_ID, READER_PHONE, targetTime, null))
                .thenReturn(Collections.singletonList(targetMsgId));

        List<ReadReceiptResult> results1 = service.markReadUpTo(ROOM_ID, targetMsgId, READER_PHONE);
        assertThat(results1).hasSize(1);
        assertThat(results1.get(0).isNewRead()).isTrue();

        // Second call: maxReadTimestamp is now targetTime (simulating updated DB state)
        when(repository.findMaxReadMessageTimestamp(ROOM_ID, READER_PHONE)).thenReturn(targetTime);
        // And unread messages is empty (or we shouldn't even look if timestamp check
        // works)

        List<ReadReceiptResult> results2 = service.markReadUpTo(ROOM_ID, targetMsgId, READER_PHONE);
        assertThat(results2).isEmpty();
    }
}
