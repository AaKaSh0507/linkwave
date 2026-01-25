package com.linkwave.app.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkwave.app.domain.auth.AuthenticatedUserContext;
import com.linkwave.app.domain.websocket.WsMessageEnvelope;
import com.linkwave.app.service.session.SessionService;
import com.linkwave.app.service.websocket.WsSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Integration tests for WebSocket gateway.
 * 
 * Tests:
 * - Connection rejection for unauthenticated users
 * - Connection acceptance for authenticated users
 * - ping/pong messaging
 * - chat.send acceptance (without delivery in C1)
 * - Invalid JSON handling
 * - Missing event field handling
 * - Unknown event handling
 * - Disconnect tracking
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WsGatewayIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @MockBean
    private SessionService sessionService;
    
    @Autowired
    private WsSessionManager sessionManager;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private String wsUrl;
    
    @BeforeEach
    void setUp() {
        wsUrl = "ws://localhost:" + port + "/ws";
    }
    
    @Test
    void connectUnauthenticated_shouldRejectConnection() throws Exception {
        // Given: No authenticated session
        when(sessionService.getAuthenticatedUser()).thenReturn(Optional.empty());
        
        // When/Then: Connection should be rejected
        StandardWebSocketClient client = new StandardWebSocketClient();
        TestWebSocketHandler handler = new TestWebSocketHandler();
        
        try {
            client.execute(handler, new WebSocketHttpHeaders(), URI.create(wsUrl)).get(5, TimeUnit.SECONDS);
            assertThat(false).as("Connection should have been rejected").isTrue();
        } catch (Exception e) {
            // Expected - connection rejected
            assertThat(e.getMessage()).contains("401");
        }
    }
    
    @Test
    void connectAuthenticated_shouldAcceptConnection() throws Exception {
        // Given: Authenticated session
        String phoneNumber = "+14155552671";
        AuthenticatedUserContext userContext = new AuthenticatedUserContext(phoneNumber, Instant.now());
        when(sessionService.getAuthenticatedUser()).thenReturn(Optional.of(userContext));
        
        // When: Connect to WebSocket
        StandardWebSocketClient client = new StandardWebSocketClient();
        TestWebSocketHandler handler = new TestWebSocketHandler();
        
        WebSocketSession session = client.execute(handler, new WebSocketHttpHeaders(), URI.create(wsUrl))
                .get(5, TimeUnit.SECONDS);
        
        // Then: Connection accepted
        assertThat(session.isOpen()).isTrue();
        assertThat(sessionManager.hasActiveSession(phoneNumber)).isTrue();
        
        // Cleanup
        session.close();
        Thread.sleep(100); // Give time for close to process
        assertThat(sessionManager.hasActiveSession(phoneNumber)).isFalse();
    }
    
    @Test
    void sendPing_shouldReceivePong() throws Exception {
        // Given: Authenticated WebSocket connection
        String phoneNumber = "+14155552672";
        AuthenticatedUserContext userContext = new AuthenticatedUserContext(phoneNumber, Instant.now());
        when(sessionService.getAuthenticatedUser()).thenReturn(Optional.of(userContext));
        
        StandardWebSocketClient client = new StandardWebSocketClient();
        TestWebSocketHandler handler = new TestWebSocketHandler();
        
        WebSocketSession session = client.execute(handler, new WebSocketHttpHeaders(), URI.create(wsUrl))
                .get(5, TimeUnit.SECONDS);
        
        // When: Send ping
        WsMessageEnvelope ping = new WsMessageEnvelope("ping", null, null);
        String pingJson = objectMapper.writeValueAsString(ping);
        session.sendMessage(new TextMessage(pingJson));
        
        // Then: Should receive pong
        String response = handler.messages.poll(5, TimeUnit.SECONDS);
        assertThat(response).isNotNull();
        
        WsMessageEnvelope pong = objectMapper.readValue(response, WsMessageEnvelope.class);
        assertThat(pong.getEvent()).isEqualTo("pong");
        
        // Cleanup
        session.close();
    }
    
    @Test
    void sendChatSend_shouldBeAccepted() throws Exception {
        // Given: Authenticated WebSocket connection
        String phoneNumber = "+14155552673";
        AuthenticatedUserContext userContext = new AuthenticatedUserContext(phoneNumber, Instant.now());
        when(sessionService.getAuthenticatedUser()).thenReturn(Optional.of(userContext));
        
        StandardWebSocketClient client = new StandardWebSocketClient();
        TestWebSocketHandler handler = new TestWebSocketHandler();
        
        WebSocketSession session = client.execute(handler, new WebSocketHttpHeaders(), URI.create(wsUrl))
                .get(5, TimeUnit.SECONDS);
        
        // When: Send chat.send message
        String chatPayload = "{\"text\":\"Hello World\"}";
        WsMessageEnvelope chatSend = new WsMessageEnvelope(
            "chat.send", 
            "+14155559999", 
            objectMapper.readTree(chatPayload)
        );
        String chatJson = objectMapper.writeValueAsString(chatSend);
        session.sendMessage(new TextMessage(chatJson));
        
        // Then: Message accepted (no error, connection remains open)
        Thread.sleep(500); // Give time for processing
        assertThat(session.isOpen()).isTrue();
        
        // Note: In C1, chat.send is accepted but not delivered
        // C2 will implement Kafka integration for delivery
        
        // Cleanup
        session.close();
    }
    
    @Test
    void sendInvalidJson_shouldCloseConnection() throws Exception {
        // Given: Authenticated WebSocket connection
        String phoneNumber = "+14155552674";
        AuthenticatedUserContext userContext = new AuthenticatedUserContext(phoneNumber, Instant.now());
        when(sessionService.getAuthenticatedUser()).thenReturn(Optional.of(userContext));
        
        StandardWebSocketClient client = new StandardWebSocketClient();
        TestWebSocketHandler handler = new TestWebSocketHandler();
        
        WebSocketSession session = client.execute(handler, new WebSocketHttpHeaders(), URI.create(wsUrl))
                .get(5, TimeUnit.SECONDS);
        
        // When: Send invalid JSON
        session.sendMessage(new TextMessage("{invalid json}"));
        
        // Then: Connection should close with 1003
        Thread.sleep(500);
        assertThat(session.isOpen()).isFalse();
    }
    
    @Test
    void sendMessageWithoutEventField_shouldCloseConnection() throws Exception {
        // Given: Authenticated WebSocket connection
        String phoneNumber = "+14155552675";
        AuthenticatedUserContext userContext = new AuthenticatedUserContext(phoneNumber, Instant.now());
        when(sessionService.getAuthenticatedUser()).thenReturn(Optional.of(userContext));
        
        StandardWebSocketClient client = new StandardWebSocketClient();
        TestWebSocketHandler handler = new TestWebSocketHandler();
        
        WebSocketSession session = client.execute(handler, new WebSocketHttpHeaders(), URI.create(wsUrl))
                .get(5, TimeUnit.SECONDS);
        
        // When: Send message without event field
        String messageWithoutEvent = "{\"to\":\"+1234567890\",\"payload\":{}}";
        session.sendMessage(new TextMessage(messageWithoutEvent));
        
        // Then: Connection should close with 1003
        Thread.sleep(500);
        assertThat(session.isOpen()).isFalse();
    }
    
    @Test
    void sendUnknownEvent_shouldIgnore() throws Exception {
        // Given: Authenticated WebSocket connection
        String phoneNumber = "+14155552676";
        AuthenticatedUserContext userContext = new AuthenticatedUserContext(phoneNumber, Instant.now());
        when(sessionService.getAuthenticatedUser()).thenReturn(Optional.of(userContext));
        
        StandardWebSocketClient client = new StandardWebSocketClient();
        TestWebSocketHandler handler = new TestWebSocketHandler();
        
        WebSocketSession session = client.execute(handler, new WebSocketHttpHeaders(), URI.create(wsUrl))
                .get(5, TimeUnit.SECONDS);
        
        // When: Send unknown event
        WsMessageEnvelope unknownEvent = new WsMessageEnvelope("unknown.event", null, null);
        String unknownJson = objectMapper.writeValueAsString(unknownEvent);
        session.sendMessage(new TextMessage(unknownJson));
        
        // Then: Connection should remain open (unknown events are ignored)
        Thread.sleep(500);
        assertThat(session.isOpen()).isTrue();
        
        // Cleanup
        session.close();
    }
    
    /**
     * Test handler to capture WebSocket messages.
     */
    private static class TestWebSocketHandler extends TextWebSocketHandler {
        
        final BlockingQueue<String> messages = new ArrayBlockingQueue<>(10);
        
        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            messages.offer(message.getPayload());
        }
    }
}
