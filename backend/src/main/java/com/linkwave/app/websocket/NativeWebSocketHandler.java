package com.linkwave.app.websocket;

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
 */
@Component
public class NativeWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NativeWebSocketHandler.class);

    // Map of phoneNumber -> WebSocketSession for active connections
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

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

        // TODO: Parse message and handle different event types
        // For now, just echo back to acknowledge receipt
        sendMessage(session, "{\"type\":\"message.ack\",\"received\":true}");
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        String phoneNumber = (String) session.getAttributes().get("phoneNumber");

        if (phoneNumber != null) {
            activeSessions.remove(phoneNumber);
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
