package com.linkwave.app.service.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing WebSocket session mappings.
 * Stores user phoneNumber -> WebSocketSession associations in memory.
 * 
 * Phase C1: In-memory storage (no Redis yet)
 * Phase C2: Will integrate with Kafka for message delivery
 */
@Service
public class WsSessionManager {
    
    private static final Logger log = LoggerFactory.getLogger(WsSessionManager.class);
    
    // phoneNumber -> WebSocketSession mapping
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    // sessionId -> phoneNumber reverse mapping for quick lookup
    private final Map<String, String> sessionToPhone = new ConcurrentHashMap<>();
    
    /**
     * Register a new WebSocket session for a user.
     * 
     * @param phoneNumber the user's phone number
     * @param session the WebSocket session
     */
    public void registerSession(String phoneNumber, WebSocketSession session) {
        // Close any existing session for this user (enforce single connection)
        WebSocketSession existingSession = sessions.get(phoneNumber);
        if (existingSession != null && existingSession.isOpen()) {
            log.info("Closing existing session for user: {}", maskPhoneNumber(phoneNumber));
            try {
                existingSession.close();
            } catch (Exception e) {
                log.warn("Error closing existing session: {}", e.getMessage());
            }
        }
        
        sessions.put(phoneNumber, session);
        sessionToPhone.put(session.getId(), phoneNumber);
        
        log.info("Registered WebSocket session for user: {} (sessionId: {})", 
                 maskPhoneNumber(phoneNumber), session.getId());
    }
    
    /**
     * Deregister a WebSocket session.
     * 
     * @param session the WebSocket session to deregister
     */
    public void deregisterSession(WebSocketSession session) {
        String phoneNumber = sessionToPhone.remove(session.getId());
        if (phoneNumber != null) {
            sessions.remove(phoneNumber);
            log.info("Deregistered WebSocket session for user: {} (sessionId: {})", 
                     maskPhoneNumber(phoneNumber), session.getId());
        }
    }
    
    /**
     * Get WebSocket session for a user.
     * 
     * @param phoneNumber the user's phone number
     * @return optional WebSocket session
     */
    public Optional<WebSocketSession> getSession(String phoneNumber) {
        WebSocketSession session = sessions.get(phoneNumber);
        if (session != null && session.isOpen()) {
            return Optional.of(session);
        } else if (session != null) {
            // Session exists but is closed, clean it up
            sessions.remove(phoneNumber);
            sessionToPhone.remove(session.getId());
        }
        return Optional.empty();
    }
    
    /**
     * Get phone number for a WebSocket session.
     * 
     * @param session the WebSocket session
     * @return optional phone number
     */
    public Optional<String> getPhoneNumber(WebSocketSession session) {
        return Optional.ofNullable(sessionToPhone.get(session.getId()));
    }
    
    /**
     * Check if a user has an active WebSocket session.
     * 
     * @param phoneNumber the user's phone number
     * @return true if user has an active session
     */
    public boolean hasActiveSession(String phoneNumber) {
        return getSession(phoneNumber).isPresent();
    }
    
    /**
     * Get total number of active sessions.
     * 
     * @return number of active sessions
     */
    public int getActiveSessionCount() {
        return sessions.size();
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
