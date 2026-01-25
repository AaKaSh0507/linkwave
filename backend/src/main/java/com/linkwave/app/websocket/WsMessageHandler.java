package com.linkwave.app.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkwave.app.domain.websocket.WsMessageEnvelope;
import com.linkwave.app.service.session.SessionService;
import com.linkwave.app.service.websocket.WsSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket handler for realtime messaging.
 * Handles connection lifecycle, message routing, and application protocol.
 * 
 * Phase C1: Direct WebSocket gateway with session-based auth
 * - No Kafka integration yet
 * - No message persistence yet
 * - Validates and routes messages according to envelope protocol
 */
@Component
public class WsMessageHandler extends TextWebSocketHandler {
    
    private static final Logger log = LoggerFactory.getLogger(WsMessageHandler.class);
    
    private final WsSessionManager sessionManager;
    private final SessionService sessionService;
    private final ObjectMapper objectMapper;
    
    public WsMessageHandler(WsSessionManager sessionManager, 
                           SessionService sessionService,
                           ObjectMapper objectMapper) {
        this.sessionManager = sessionManager;
        this.sessionService = sessionService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Handle new WebSocket connection.
     * Registers session if authenticated.
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String phoneNumber = (String) session.getAttributes().get("phoneNumber");
        
        if (phoneNumber == null) {
            log.warn("Connection established but phoneNumber is null (sessionId: {})", session.getId());
            session.close(new CloseStatus(1008, "Policy violation: Not authenticated"));
            return;
        }
        
        // Register session
        sessionManager.registerSession(phoneNumber, session);
        
        log.info("WebSocket connection established for user: {} (sessionId: {})", 
                 maskPhoneNumber(phoneNumber), session.getId());
    }
    
    /**
     * Handle incoming WebSocket messages.
     * Parses JSON envelope and routes according to event type.
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        String phoneNumber = (String) session.getAttributes().get("phoneNumber");
        
        log.debug("Received message from user: {} (sessionId: {})", 
                  maskPhoneNumber(phoneNumber), session.getId());
        
        // Parse message envelope
        WsMessageEnvelope envelope;
        try {
            envelope = objectMapper.readValue(payload, WsMessageEnvelope.class);
        } catch (Exception e) {
            log.warn("Invalid JSON received from user: {} - {}", 
                     maskPhoneNumber(phoneNumber), e.getMessage());
            session.close(new CloseStatus(1003, "Invalid JSON format"));
            return;
        }
        
        // Validate event field
        if (envelope.getEvent() == null || envelope.getEvent().isBlank()) {
            log.warn("Missing event field from user: {}", maskPhoneNumber(phoneNumber));
            session.close(new CloseStatus(1003, "Missing event field"));
            return;
        }
        
        // Route message based on event type
        String event = envelope.getEvent();
        
        if (!WsMessageEnvelope.EventType.isValid(event)) {
            log.warn("Unknown event type '{}' from user: {}", event, maskPhoneNumber(phoneNumber));
            // Unknown events are ignored with warning (don't close connection)
            return;
        }
        
        WsMessageEnvelope.EventType eventType = WsMessageEnvelope.EventType.fromString(event);
        
        switch (eventType) {
            case PING -> handlePing(session, phoneNumber);
            case PONG -> handlePong(session, phoneNumber);
            case CHAT_SEND -> handleChatSend(session, phoneNumber, envelope);
            default -> log.warn("Unhandled event type: {}", event);
        }
    }
    
    /**
     * Handle WebSocket connection close.
     * Deregisters session.
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String phoneNumber = (String) session.getAttributes().get("phoneNumber");
        
        sessionManager.deregisterSession(session);
        
        log.info("WebSocket connection closed for user: {} (sessionId: {}, status: {})", 
                 maskPhoneNumber(phoneNumber), session.getId(), status);
    }
    
    /**
     * Handle transport errors.
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String phoneNumber = (String) session.getAttributes().get("phoneNumber");
        
        log.error("WebSocket transport error for user: {} (sessionId: {}): {}", 
                  maskPhoneNumber(phoneNumber), session.getId(), exception.getMessage());
        
        sessionManager.deregisterSession(session);
        
        if (session.isOpen()) {
            session.close(new CloseStatus(1011, "Server error"));
        }
    }
    
    /**
     * Handle ping event.
     * Responds with pong.
     */
    private void handlePing(WebSocketSession session, String phoneNumber) throws Exception {
        log.debug("Received ping from user: {}", maskPhoneNumber(phoneNumber));
        
        // Create pong response
        WsMessageEnvelope pongEnvelope = new WsMessageEnvelope("pong", null, null);
        String pongJson = objectMapper.writeValueAsString(pongEnvelope);
        
        session.sendMessage(new TextMessage(pongJson));
        
        log.debug("Sent pong to user: {}", maskPhoneNumber(phoneNumber));
    }
    
    /**
     * Handle pong event.
     * Client sent pong (acknowledgment).
     */
    private void handlePong(WebSocketSession session, String phoneNumber) {
        log.debug("Received pong from user: {}", maskPhoneNumber(phoneNumber));
        // Pong received - connection is alive
        // No further action needed for C1
    }
    
    /**
     * Handle chat.send event.
     * Phase C1: Accept and validate message, but don't deliver yet.
     * Phase C2: Will integrate with Kafka for delivery.
     */
    private void handleChatSend(WebSocketSession session, String phoneNumber, WsMessageEnvelope envelope) {
        log.info("Received chat.send from user: {} to: {}", 
                 maskPhoneNumber(phoneNumber), 
                 envelope.getTo() != null ? maskPhoneNumber(envelope.getTo()) : "null");
        
        // Basic validation
        if (envelope.getTo() == null || envelope.getTo().isBlank()) {
            log.warn("chat.send missing 'to' field from user: {}", maskPhoneNumber(phoneNumber));
            return;
        }
        
        if (envelope.getPayload() == null) {
            log.warn("chat.send missing 'payload' field from user: {}", maskPhoneNumber(phoneNumber));
            return;
        }
        
        // Phase C1: Message accepted but queued for later processing
        // No delivery yet - will be implemented in C2 with Kafka
        log.info("chat.send accepted from user: {} (not delivered in C1)", maskPhoneNumber(phoneNumber));
        
        // TODO C2: Send to Kafka topic for processing and delivery
    }
    
    /**
     * Mask phone number for logging (show first 4 chars and last 2).
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 7) {
            return "***";
        }
        return phoneNumber.substring(0, 4) + "***" + phoneNumber.substring(phoneNumber.length() - 2);
    }
}
