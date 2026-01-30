package com.linkwave.app.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkwave.app.service.presence.PresenceService;
import com.linkwave.app.service.readreceipt.ReadReceiptService;
import com.linkwave.app.service.room.RoomMembershipService;
import com.linkwave.app.service.typing.TypingStateManager;
import com.linkwave.app.service.websocket.WsSessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NativeWebSocketHandlerTypingTest {

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

    @Mock
    private WsSessionManager sessionManager;

    private ObjectMapper objectMapper; // Real ObjectMapper for JSON parsing

    @Mock
    private WebSocketSession session;

    private AutoCloseable mocks;

    private static final String TEST_PHONE = "+14155551234";
    private static final String TEST_PHONE_2 = "+14155555678";
    private static final String TEST_ROOM = "room-123";
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
                sessionManager,
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
    void testTypingStart_validRoom_broadcastsToMembers() throws Exception {

        when(roomMembershipService.isUserInRoom(TEST_PHONE, TEST_ROOM)).thenReturn(true);
        when(typingStateManager.markTypingStart(TEST_ROOM, TEST_PHONE, SESSION_ID)).thenReturn(true);
        when(roomMembershipService.getRoomMembers(TEST_ROOM))
                .thenReturn(Set.of(TEST_PHONE, TEST_PHONE_2));

        TextMessage message = new TextMessage("{\"event\":\"typing.start\",\"roomId\":\"" + TEST_ROOM + "\"}");
        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session, message);

        verify(typingStateManager).markTypingStart(TEST_ROOM, TEST_PHONE, SESSION_ID);
    }

    @Test
    void testTypingStart_notRoomMember_ignored() throws Exception {

        when(roomMembershipService.isUserInRoom(TEST_PHONE, TEST_ROOM)).thenReturn(false);

        TextMessage message = new TextMessage("{\"event\":\"typing.start\",\"roomId\":\"" + TEST_ROOM + "\"}");
        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session, message);

        verify(typingStateManager, never()).markTypingStart(anyString(), anyString(), anyString());
    }

    @Test
    void testTypingStart_rateLimited_notBroadcast() throws Exception {

        when(roomMembershipService.isUserInRoom(TEST_PHONE, TEST_ROOM)).thenReturn(true);
        when(typingStateManager.markTypingStart(TEST_ROOM, TEST_PHONE, SESSION_ID)).thenReturn(false);

        TextMessage message = new TextMessage("{\"event\":\"typing.start\",\"roomId\":\"" + TEST_ROOM + "\"}");
        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session, message);

        verify(roomMembershipService, never()).getRoomMembers(anyString());
    }

    @Test
    void testTypingStop_broadcastsToMembers() throws Exception {

        when(roomMembershipService.getRoomMembers(TEST_ROOM))
                .thenReturn(Set.of(TEST_PHONE, TEST_PHONE_2));

        TextMessage message = new TextMessage("{\"event\":\"typing.stop\",\"roomId\":\"" + TEST_ROOM + "\"}");
        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session, message);

        verify(typingStateManager).markTypingStop(TEST_ROOM, TEST_PHONE, SESSION_ID);
    }

    @Test
    void testDisconnect_clearsTypingAndBroadcasts() throws Exception {

        when(typingStateManager.clearUserTyping(TEST_PHONE, SESSION_ID))
                .thenReturn(List.of(TEST_ROOM));
        when(roomMembershipService.getRoomMembers(TEST_ROOM))
                .thenReturn(Set.of(TEST_PHONE, TEST_PHONE_2));

        handler.afterConnectionEstablished(session);
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(typingStateManager).clearUserTyping(TEST_PHONE, SESSION_ID);
    }

    @Test
    void testTypingStart_missingRoomId_ignored() throws Exception {

        TextMessage message = new TextMessage("{\"event\":\"typing.start\"}");
        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session, message);

        verify(typingStateManager, never()).markTypingStart(anyString(), anyString(), anyString());
    }

    @Test
    void testTypingStop_missingRoomId_ignored() throws Exception {

        TextMessage message = new TextMessage("{\"event\":\"typing.stop\"}");
        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session, message);

        verify(typingStateManager, never()).markTypingStop(anyString(), anyString(), anyString());
    }

    @Test
    void testSenderDoesNotReceiveOwnTypingEvent() throws Exception {

        when(roomMembershipService.isUserInRoom(TEST_PHONE, TEST_ROOM)).thenReturn(true);
        when(typingStateManager.markTypingStart(TEST_ROOM, TEST_PHONE, SESSION_ID)).thenReturn(true);
        when(roomMembershipService.getRoomMembers(TEST_ROOM))
                .thenReturn(Set.of(TEST_PHONE, TEST_PHONE_2));

        TextMessage message = new TextMessage("{\"event\":\"typing.start\",\"roomId\":\"" + TEST_ROOM + "\"}");
        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session, message);

        verify(roomMembershipService).getRoomMembers(TEST_ROOM);
    }
}
