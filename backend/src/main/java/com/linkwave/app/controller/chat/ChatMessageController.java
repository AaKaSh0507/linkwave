package com.linkwave.app.controller.chat;

import com.linkwave.app.domain.chat.ChatMessage;
import com.linkwave.app.service.chat.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * STOMP message controller for chat operations.
 * 
 * Phase D: Real-time messaging with STOMP
 * 
 * Message Flow:
 * 1. Client sends message to /app/chat.send
 * 2. Controller validates and publishes to Kafka
 * 3. Kafka consumer persists and broadcasts to room
 * 4. All room members receive via /topic/room.{roomId}
 */
@Controller
public class ChatMessageController {
    
    private static final Logger log = LoggerFactory.getLogger(ChatMessageController.class);
    
    private final ChatService chatService;
    
    public ChatMessageController(ChatService chatService) {
        this.chatService = chatService;
    }
    
    /**
     * Handle incoming chat messages from clients.
     * 
     * @param payload Message payload containing roomId and body
     * @param principal Authenticated user (phone number)
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload SendMessagePayload payload, Principal principal) {
        String senderPhoneNumber = principal.getName();
        
        log.info("Received message from {} to room {}", maskPhone(senderPhoneNumber), payload.roomId());
        
        try {
            // Validate and publish to Kafka
            chatService.sendMessage(payload.roomId(), senderPhoneNumber, payload.body());
        } catch (Exception e) {
            log.error("Failed to send message: {}", e.getMessage());
            // In production, send error back to user via /user/queue/errors
            throw new RuntimeException("Failed to send message: " + e.getMessage());
        }
    }
    
    /**
     * Payload for sending a message.
     */
    public record SendMessagePayload(String roomId, String body) {}
    
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        return phone.substring(0, 4) + "***";
    }
}
