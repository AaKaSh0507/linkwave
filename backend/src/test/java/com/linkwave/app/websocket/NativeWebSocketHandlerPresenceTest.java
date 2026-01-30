package com.linkwave.app.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkwave.app.service.presence.PresenceService;
import com.linkwave.app.service.readreceipt.ReadReceiptService;
import com.linkwave.app.service.room.RoomMembershipService;
import com.linkwave.app.service.typing.TypingStateManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class NativeWebSocketHandlerPresenceTest {

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
    private ObjectMapper objectMapper;

    @Mock
    private WebSocketSession session;

    private AutoCloseable mocks;

    private static final String TEST_PHONE = "+14155551234";

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        handler = new NativeWebSocketHandler(
                presenceService,
                typingStateManager,
                roomMembershipService,
                readReceiptService,
                chatService,
                objectMapper);

        when(session.getId()).thenReturn("test-session-id");
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
    void testConnectionEstablished_marksUserOnline() throws Exception {

        handler.afterConnectionEstablished(session);

        verify(presenceService, times(1)).markOnline(TEST_PHONE);
    }

    @Test
    void testConnectionClosed_marksUserDisconnect() throws Exception {

        handler.afterConnectionEstablished(session);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(presenceService, times(1)).markDisconnect(TEST_PHONE);
    }

    @Test
    void testHeartbeatMessage_recordsHeartbeat() throws Exception {

        handler.afterConnectionEstablished(session);
        when(presenceService.recordHeartbeat(anyString())).thenReturn(true);

        TextMessage heartbeatMsg = new TextMessage("{\"type\":\"presence.heartbeat\"}");
        handler.handleTextMessage(session, heartbeatMsg);

        verify(presenceService, times(1)).recordHeartbeat(TEST_PHONE);
    }

    @Test
    void testHeartbeatMessage_sendsAck_whenSuccessful() throws Exception {

        handler.afterConnectionEstablished(session);
        when(presenceService.recordHeartbeat(anyString())).thenReturn(true);

        TextMessage heartbeatMsg = new TextMessage("{\"type\":\"presence.heartbeat\"}");
        handler.handleTextMessage(session, heartbeatMsg);

        verify(session, atLeastOnce()).sendMessage(argThat(msg -> {
            if (msg instanceof TextMessage) {
                String payload = ((TextMessage) msg).getPayload();
                return payload.contains("presence.heartbeat.ack") && payload.contains("\"status\":\"ok\"");
            }
            return false;
        }));
    }

    @Test
    void testHeartbeatMessage_sendsRateLimitedAck_whenRateLimited() throws Exception {

        handler.afterConnectionEstablished(session);
        when(presenceService.recordHeartbeat(anyString())).thenReturn(false);

        TextMessage heartbeatMsg = new TextMessage("{\"type\":\"presence.heartbeat\"}");
        handler.handleTextMessage(session, heartbeatMsg);

        verify(session, atLeastOnce()).sendMessage(argThat(msg -> {
            if (msg instanceof TextMessage) {
                String payload = ((TextMessage) msg).getPayload();
                return payload.contains("presence.heartbeat.ack") && payload.contains("\"status\":\"rate_limited\"");
            }
            return false;
        }));
    }

    @Test
    void testMultipleConnections_eachMarkedOnline() throws Exception {

        handler.afterConnectionEstablished(session);

        WebSocketSession session2 = mock(WebSocketSession.class);
        when(session2.getId()).thenReturn("test-session-id-2");
        when(session2.isOpen()).thenReturn(true);
        Map<String, Object> attributes2 = new HashMap<>();
        attributes2.put("phoneNumber", TEST_PHONE);
        when(session2.getAttributes()).thenReturn(attributes2);

        handler.afterConnectionEstablished(session2);

        verify(presenceService, times(2)).markOnline(TEST_PHONE);
    }

    @Test
    void testConnectionWithoutPhoneNumber_doesNotMarkOnline() throws Exception {

        Map<String, Object> emptyAttributes = new HashMap<>();
        when(session.getAttributes()).thenReturn(emptyAttributes);

        handler.afterConnectionEstablished(session);

        verify(presenceService, never()).markOnline(anyString());
    }
}
