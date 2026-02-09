package com.linkwave.app.service.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WsSessionManager {

    private static final Logger log = LoggerFactory.getLogger(WsSessionManager.class);
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToPhone = new ConcurrentHashMap<>();

    public void registerSession(String phoneNumber, WebSocketSession session) {
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

    public void deregisterSession(WebSocketSession session) {
        String phoneNumber = sessionToPhone.remove(session.getId());
        if (phoneNumber != null) {
            sessions.remove(phoneNumber);
            log.info("Deregistered WebSocket session for user: {} (sessionId: {})",
                     maskPhoneNumber(phoneNumber), session.getId());
        }
    }

    public Optional<WebSocketSession> getSession(String phoneNumber) {
        WebSocketSession session = sessions.get(phoneNumber);
        if (session != null && session.isOpen()) {
            return Optional.of(session);
        } else if (session != null) {
            sessions.remove(phoneNumber);
            sessionToPhone.remove(session.getId());
        }
        return Optional.empty();
    }

    public Optional<String> getPhoneNumber(WebSocketSession session) {
        return Optional.ofNullable(sessionToPhone.get(session.getId()));
    }

    public boolean hasActiveSession(String phoneNumber) {
        return getSession(phoneNumber).isPresent();
    }

    public int getActiveSessionCount() {
        return sessions.size();
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 7) {
            return "***";
        }
        return phoneNumber.substring(0, 4) + "***" + phoneNumber.substring(phoneNumber.length() - 2);
    }
}
