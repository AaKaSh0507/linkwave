package com.linkwave.app.service.readreceipt;

import com.linkwave.app.domain.chat.ChatMessageEntity;
import com.linkwave.app.domain.chat.ReadReceiptEntity;
import com.linkwave.app.exception.UnauthorizedException;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadReceiptServiceTest {

    @Mock
    private ReadReceiptRepository repository;
    @Mock
    private RoomMembershipService roomMembershipService;
    @Mock
    private ChatMessageRepository messageRepository;

    private ReadReceiptService service;

    private static final String ROOM_ID = "room-123";
    private static final String MESSAGE_ID = "msg-456";
    private static final String READER_PHONE = "+15550000000";

    @BeforeEach
    void setUp() {
        service = new ReadReceiptService(repository, roomMembershipService, messageRepository);
    }

    @Test
    void markMessageRead_whenNewRead_shouldPersistAndReturnTrue() {
        
        when(repository.existsByMessageIdAndReaderPhoneNumber(MESSAGE_ID, READER_PHONE)).thenReturn(false);
        when(roomMembershipService.isUserInRoom(READER_PHONE, ROOM_ID)).thenReturn(true);

        
        ReadReceiptResult result = service.markMessageRead(MESSAGE_ID, ROOM_ID, READER_PHONE);

        
        assertThat(result.isNewRead()).isTrue();
        assertThat(result.getReceipt()).isNotNull();
        assertThat(result.getReceipt().getMessageId()).isEqualTo(MESSAGE_ID);
        assertThat(result.getReceipt().getReaderPhoneNumber()).isEqualTo(READER_PHONE);
        verify(repository).save(any(ReadReceiptEntity.class));
    }

    @Test
    void markMessageRead_whenAlreadyRead_shouldReturnFalse() {
        
        when(repository.existsByMessageIdAndReaderPhoneNumber(MESSAGE_ID, READER_PHONE)).thenReturn(true);

        
        ReadReceiptResult result = service.markMessageRead(MESSAGE_ID, ROOM_ID, READER_PHONE);

        
        assertThat(result.isNewRead()).isFalse();
        verify(repository, never()).save(any());
    }

    @Test
    void markMessageRead_whenUserNotInRoom_shouldThrowException() {
        
        when(repository.existsByMessageIdAndReaderPhoneNumber(MESSAGE_ID, READER_PHONE)).thenReturn(false);
        when(roomMembershipService.isUserInRoom(READER_PHONE, ROOM_ID)).thenReturn(false);

        
        assertThrows(UnauthorizedException.class, () -> service.markMessageRead(MESSAGE_ID, ROOM_ID, READER_PHONE));
    }

    @Test
    void markReadUpTo_shouldMarkAllUnreadMessages() {
        
        String targetMsgId = "msg-target";
        Instant targetTime = Instant.now();

        ChatMessageEntity targetMsg = new ChatMessageEntity();
        targetMsg.setId(targetMsgId);
        targetMsg.setSentAt(targetTime);

        when(messageRepository.findById(targetMsgId)).thenReturn(Optional.of(targetMsg));

        
        
        when(repository.findMaxReadMessageTimestamp(ROOM_ID, READER_PHONE)).thenReturn(null);

        
        List<String> unreadIds = Arrays.asList("msg-1", "msg-2", targetMsgId);
        when(repository.findUnreadMessageIdsUpTo(ROOM_ID, READER_PHONE, targetTime))
                .thenReturn(unreadIds);

        when(repository.existsByMessageIdAndReaderPhoneNumber(any(), any())).thenReturn(false);
        when(roomMembershipService.isUserInRoom(READER_PHONE, ROOM_ID)).thenReturn(true);

        
        List<ReadReceiptResult> results = service.markReadUpTo(ROOM_ID, targetMsgId, READER_PHONE);

        
        assertThat(results).hasSize(3);
        verify(repository, times(3)).save(any(ReadReceiptEntity.class));
    }
}
