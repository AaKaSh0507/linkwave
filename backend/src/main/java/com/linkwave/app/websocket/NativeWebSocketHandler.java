package com.linkwave.app.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkwave.app.service.chat.ChatService;
import com.linkwave.app.service.presence.PresenceService;
import com.linkwave.app.service.readreceipt.ReadReceiptService;
import com.linkwave.app.service.room.RoomMembershipService;
import com.linkwave.app.service.typing.TypingStateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Native WebSocket handler for real-time messaging.
 * 
 * Handles WebSocket connections at /ws endpoint using native WebSocket protocol
 * (not STOMP). Messages are exchanged as JSON text frames.
 * 
 * Authentication:
 * - Session-based authentication via WsAuthenticationInterceptor
 * - Phone number available in session attributes
 * 
 * Message Flow:
 * - Client connects with session cookie
 * - Client sends JSON messages
 * - Server broadcasts to relevant recipients
 * 
 * Phase D1: Presence Tracking
 * - Tracks user online/offline status via PresenceService
 * - Handles presence.heartbeat messages to refresh TTL
 * - Multi-device support via connection counting
 */
@Component
public class NativeWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NativeWebSocketHandler.class);

    // Map of phoneNumber -> WebSocketSession for active connections
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    private final PresenceService presenceService;
    private final TypingStateManager typingStateManager;
    private final RoomMembershipService roomMembershipService;
    private final ReadReceiptService readReceiptService;
    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    public NativeWebSocketHandler(
            PresenceService presenceService,
            TypingStateManager typingStateManager,
            RoomMembershipService roomMembershipService,
            ReadReceiptService readReceiptService,
            ChatService chatService,
            ObjectMapper objectMapper) {
        this.presenceService = presenceService;
        this.typingStateManager = typingStateManager;
        this.roomMembershipService = roomMembershipService;
        this.readReceiptService = readReceiptService;
        this.chatService = chatService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        String phoneNumber = (String) session.getAttributes().get("phoneNumber");

        if (phoneNumber == null) {
            log.warn("WebSocket connection without phoneNumber attribute, closing");
            @SuppressWarnings("nullness")
            CloseStatus policyViolation = CloseStatus.POLICY_VIOLATION;
            session.close(policyViolation);
            return;
        }

        // Store session
        activeSessions.put(phoneNumber, session);

        // Mark user as online (Phase D1: Presence Tracking)
        presenceService.markOnline(phoneNumber);

        log.info("WebSocket connection established for user: {} (sessionId: {})",
                maskPhoneNumber(phoneNumber), session.getId());

        // Send connection acknowledgment
        sendMessage(session, "{\"type\":\"connection.ack\",\"status\":\"connected\"}");
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        String phoneNumber = (String) session.getAttributes().get("phoneNumber");
        String payload = message.getPayload();

        log.debug("Received message from {}: {}", maskPhoneNumber(phoneNumber), payload);

        // Parse message type
        try {
            JsonNode jsonNode = objectMapper.readTree(payload);
            String messageType = jsonNode.has("type") ? jsonNode.get("type").asText() : null;

            if (messageType == null) {
                log.warn("Message from {} missing 'type' field", maskPhoneNumber(phoneNumber));
                return;
            }

            // Handle different message types
            switch (messageType) {
                case "presence.heartbeat":
                    handlePresenceHeartbeat(session, phoneNumber);
                    break;

                default:
                    log.debug("Unhandled message type: {}", messageType);
                    sendMessage(session, "{\"type\":\"message.ack\",\"received\":true}");
                    break;
            }

        } catch (Exception e) {
            log.error("Failed to parse message from {}: {}", maskPhoneNumber(phoneNumber), e.getMessage());
        }
    }

    /**
     * Handle presence heartbeat message.
     * Refreshes user's presence TTL in Redis.
     */
    private void handlePresenceHeartbeat(WebSocketSession session, String phoneNumber) {
        boolean success = presenceService.recordHeartbeat(phoneNumber);

        String status = success ? "ok" : "rate_limited";
        String response = String.format("{\"type\":\"presence.heartbeat.ack\",\"status\":\"%s\"}", status);

        sendMessage(session, response);

        if (success) {
            log.debug("Heartbeat recorded for user: {}", maskPhoneNumber(phoneNumber));
        } else {
            log.debug("Heartbeat rate-limited for user: {}", maskPhoneNumber(phoneNumber));
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        String phoneNumber = (String) session.getAttributes().get("phoneNumber");

        if (phoneNumber != null) {
            activeSessions.remove(phoneNumber);

            // Mark user disconnect (Phase D1: Presence Tracking)
            // TTL will handle final offline status
            presenceService.markDisconnect(phoneNumber);

            log.info("WebSocket connection closed for user: {} (status: {})",
                    maskPhoneNumber(phoneNumber), status);
        }
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) throws Exception {
        String phoneNumber = (String) session.getAttributes().get("phoneNumber");
        log.error("WebSocket transport error for user: {}", maskPhoneNumber(phoneNumber), exception);

        if (session.isOpen()) {
            @SuppressWarnings("nullness")
            CloseStatus serverError = CloseStatus.SERVER_ERROR;
            session.close(serverError);
        }
    }

    /**
     * Send a message to a specific session.
     */
    private void sendMessage(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
                @SuppressWarnings("nullness")
                TextMessage textMessage = new TextMessage(message);
                session.sendMessage(textMessage);
            }
        } catch (IOException e) {
            log.error("Failed to send message to session {}: {}", session.getId(), e.getMessage());
        }
    }

    /**
     * Broadcast a message to a specific user by phone number.
     */
    public void sendToUser(String phoneNumber, String message) {
        WebSocketSession session = activeSessions.get(phoneNumber);
        if (session != null) {
            sendMessage(session, message);
        }
    }

    /**
     * Get count of active connections.
     */
    public int getActiveConnectionCount() {
        return activeSessions.size();
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 7) {
            return "***";
        }
        return phoneNumber.substring(0, 4) + "***" + phoneNumber.substring(phoneNumber.length() - 2);
    }
}
