package com.linkwave.app.service.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkwave.app.domain.chat.ChatMessage;
import com.linkwave.app.domain.websocket.WsMessageEnvelope;
import com.linkwave.app.service.websocket.WsSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Optional;

/**
 * Service for delivering chat messages to online users via WebSocket.
 */
@Service
public class ChatFanoutService {

    private static final Logger log = LoggerFactory.getLogger(ChatFanoutService.class);

    private final WsSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    public ChatFanoutService(WsSessionManager sessionManager, ObjectMapper objectMapper) {
        this.sessionManager = sessionManager;
        this.objectMapper = objectMapper;
    }

    /**
     * Deliver a message to the recipient if they are online.
     */
    public void deliver(ChatMessage message) {
        String recipientPhone = message.getRecipientPhoneNumber();
        Optional<WebSocketSession> sessionOpt = sessionManager.getSession(recipientPhone);

        if (sessionOpt.isPresent()) {
            WebSocketSession session = sessionOpt.get();
            try {
                // Construct chat.receive envelope
                WsMessageEnvelope envelope = new WsMessageEnvelope(
                        "chat.receive",
                        message.getRecipientPhoneNumber(), // To: user (self)
                        objectMapper.valueToTree(message));

                String json = objectMapper.writeValueAsString(envelope);
                session.sendMessage(new TextMessage(json));

                log.info("Delivered message {} to user {}", message.getMessageId(), message.getMaskedRecipient());

            } catch (IOException e) {
                log.error("Failed to deliver message {} to user {}: {}",
                        message.getMessageId(), message.getMaskedRecipient(), e.getMessage());
                // In phase E/F we might trigger push notification here or mark as undelivered
            }
        } else {
            log.debug("User {} is offline, message {} persisted but not delivered via WS",
                    message.getMaskedRecipient(), message.getMessageId());
        }
    }
}
