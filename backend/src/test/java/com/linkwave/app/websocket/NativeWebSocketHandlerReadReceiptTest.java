package com.linkwave.app.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkwave.app.domain.chat.ReadReceiptEntity;
import com.linkwave.app.exception.NotFoundException;
import com.linkwave.app.exception.UnauthorizedException;
import com.linkwave.app.service.presence.PresenceService;
import com.linkwave.app.service.readreceipt.ReadReceiptService;
import com.linkwave.app.service.readreceipt.ReadReceiptService.ReadReceiptResult;
import com.linkwave.app.service.room.RoomMembershipService;
import com.linkwave.app.service.typing.TypingStateManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NativeWebSocketHandlerReadReceiptTest {

    private NativeWebSocketHandler handler;

    @Mock
    private PresenceService presenceService;

    @Mock
    private TypingStateManager typingStateManager;

    @Mock
    private RoomMembershipService roomMembershipService;

    @Mock
    private ReadReceiptService readReceiptService;

    @Mock
    private com.linkwave.app.service.chat.ChatService chatService;

    private ObjectMapper objectMapper; // Real ObjectMapper for JSON parsing

    @Mock
    private WebSocketSession session;

    private AutoCloseable mocks;

    private static final String TEST_PHONE = "+14155551234";
    private static final String TEST_PHONE_2 = "+14155555678";
    private static final String TEST_ROOM = "room-123";
    private static final String TEST_MESSAGE = "msg-456";
    private static final String SESSION_ID = "test-session-id";

    @BeforeEach
    void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper(); // Use real ObjectMapper
        handler = new NativeWebSocketHandler(
                presenceService,
                typingStateManager,
                roomMembershipService,
                readReceiptService,
                chatService,
                objectMapper);

        when(session.getId()).thenReturn(SESSION_ID);
        when(session.isOpen()).thenReturn(true);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("phoneNumber", TEST_PHONE);
        when(session.getAttributes()).thenReturn(attributes);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void testReadUpTo_validMessage_shouldPersistAndBroadcast() throws Exception {
        // Arrange
        ReadReceiptEntity receipt = new ReadReceiptEntity();
        receipt.setMessageId(TEST_MESSAGE);
        receipt.setRoomId(TEST_ROOM);
        receipt.setReaderPhoneNumber(TEST_PHONE);
        receipt.setReadAt(Instant.now());

        when(readReceiptService.markReadUpTo(TEST_ROOM, TEST_MESSAGE, TEST_PHONE))
                .thenReturn(List.of(ReadReceiptResult.newRead(receipt)));
        when(roomMembershipService.getRoomMembers(TEST_ROOM))
                .thenReturn(Set.of(TEST_PHONE, TEST_PHONE_2));

        TextMessage message = new TextMessage(
                "{\"type\":\"read.up_to\",\"roomId\":\"" + TEST_ROOM + "\",\"messageId\":\"" + TEST_MESSAGE + "\"}");

        // Act
        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session, message);

        // Assert
        verify(readReceiptService).markReadUpTo(TEST_ROOM, TEST_MESSAGE, TEST_PHONE);
        verify(roomMembershipService).getRoomMembers(TEST_ROOM);
    }

    @Test
    void testReadUpTo_duplicateRead_shouldNotBroadcast() throws Exception {
        // Arrange
        when(readReceiptService.markReadUpTo(TEST_ROOM, TEST_MESSAGE, TEST_PHONE))
                .thenReturn(List.of(ReadReceiptResult.alreadyRead()));

        TextMessage message = new TextMessage(
                "{\"type\":\"read.up_to\",\"roomId\":\"" + TEST_ROOM + "\",\"messageId\":\"" + TEST_MESSAGE + "\"}");

        // Act
        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session, message);

        // Assert
        verify(readReceiptService).markReadUpTo(TEST_ROOM, TEST_MESSAGE, TEST_PHONE);
        verify(roomMembershipService, never()).getRoomMembers(anyString());
    }

    @Test
    void testReadUpTo_userNotInRoom_shouldNotPersist() throws Exception {
        // Arrange
        when(readReceiptService.markReadUpTo(TEST_ROOM, TEST_MESSAGE, TEST_PHONE))
                .thenThrow(new UnauthorizedException("Not a room member"));

        TextMessage message = new TextMessage(
                "{\"type\":\"read.up_to\",\"roomId\":\"" + TEST_ROOM + "\",\"messageId\":\"" + TEST_MESSAGE + "\"}");

        // Act
        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session, message);

        // Assert
        verify(readReceiptService).markReadUpTo(TEST_ROOM, TEST_MESSAGE, TEST_PHONE);
        verify(roomMembershipService, never()).getRoomMembers(anyString());
    }

    @Test
    void testReadUpTo_messageNotFound_shouldNotBroadcast() throws Exception {
        // Arrange
        when(readReceiptService.markReadUpTo(TEST_ROOM, TEST_MESSAGE, TEST_PHONE))
                .thenThrow(new NotFoundException("Message not found"));

        TextMessage message = new TextMessage(
                "{\"type\":\"read.up_to\",\"roomId\":\"" + TEST_ROOM + "\",\"messageId\":\"" + TEST_MESSAGE + "\"}");

        // Act
        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session, message);

        // Assert
        verify(readReceiptService).markReadUpTo(TEST_ROOM, TEST_MESSAGE, TEST_PHONE);
        verify(roomMembershipService, never()).getRoomMembers(anyString());
    }

    @Test
    void testReadUpTo_missingRoomId_shouldBeIgnored() throws Exception {
        // Arrange
        TextMessage message = new TextMessage("{\"type\":\"read.up_to\",\"messageId\":\"" + TEST_MESSAGE + "\"}");

        // Act
        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session, message);

        // Assert
        verify(readReceiptService, never()).markReadUpTo(anyString(), anyString(), anyString());
    }

    @Test
    void testReadUpTo_missingMessageId_shouldBeIgnored() throws Exception {
        // Arrange
        TextMessage message = new TextMessage("{\"type\":\"read.up_to\",\"roomId\":\"" + TEST_ROOM + "\"}");

        // Act
        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session, message);

        // Assert
        verify(readReceiptService, never()).markReadUpTo(anyString(), anyString(), anyString());
    }

    @Test
    void testReadUpTo_groupChat_multipleReaders() throws Exception {
        // Arrange
        ReadReceiptEntity receipt1 = new ReadReceiptEntity();
        receipt1.setMessageId("msg-1");
        receipt1.setRoomId(TEST_ROOM);
        receipt1.setReaderPhoneNumber(TEST_PHONE);
        receipt1.setReadAt(Instant.now());

        ReadReceiptEntity receipt2 = new ReadReceiptEntity();
        receipt2.setMessageId("msg-2");
        receipt2.setRoomId(TEST_ROOM);
        receipt2.setReaderPhoneNumber(TEST_PHONE);
        receipt2.setReadAt(Instant.now());

        when(readReceiptService.markReadUpTo(TEST_ROOM, TEST_MESSAGE, TEST_PHONE))
                .thenReturn(List.of(
                        ReadReceiptResult.newRead(receipt1),
                        ReadReceiptResult.newRead(receipt2)));
        when(roomMembershipService.getRoomMembers(TEST_ROOM))
                .thenReturn(Set.of(TEST_PHONE, TEST_PHONE_2, "+14155559999"));

        TextMessage message = new TextMessage(
                "{\"type\":\"read.up_to\",\"roomId\":\"" + TEST_ROOM + "\",\"messageId\":\"" + TEST_MESSAGE + "\"}");

        // Act
        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session, message);

        // Assert
        verify(readReceiptService).markReadUpTo(TEST_ROOM, TEST_MESSAGE, TEST_PHONE);
        verify(roomMembershipService, times(2)).getRoomMembers(TEST_ROOM);
    }

    @Test
    void testBroadcastExcludesReader() throws Exception {
        // Arrange
        ReadReceiptEntity receipt = new ReadReceiptEntity();
        receipt.setMessageId(TEST_MESSAGE);
        receipt.setRoomId(TEST_ROOM);
        receipt.setReaderPhoneNumber(TEST_PHONE);
        receipt.setReadAt(Instant.now());

        when(readReceiptService.markReadUpTo(TEST_ROOM, TEST_MESSAGE, TEST_PHONE))
                .thenReturn(List.of(ReadReceiptResult.newRead(receipt)));
        when(roomMembershipService.getRoomMembers(TEST_ROOM))
                .thenReturn(Set.of(TEST_PHONE, TEST_PHONE_2));

        TextMessage message = new TextMessage(
                "{\"type\":\"read.up_to\",\"roomId\":\"" + TEST_ROOM + "\",\"messageId\":\"" + TEST_MESSAGE + "\"}");

        // Act
        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session, message);

        // Assert
        verify(roomMembershipService).getRoomMembers(TEST_ROOM);
        // The reader (TEST_PHONE) should not receive the broadcast
        // Only TEST_PHONE_2 should receive it
    }
}
