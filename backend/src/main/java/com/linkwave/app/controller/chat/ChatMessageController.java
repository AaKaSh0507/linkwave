package com.linkwave.app.controller.chat;

import com.linkwave.app.domain.chat.ChatMessage;
import com.linkwave.app.service.chat.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatMessageController {

    private static final Logger log = LoggerFactory.getLogger(ChatMessageController.class);

    private final ChatService chatService;

    public ChatMessageController(ChatService chatService) {
        this.chatService = chatService;
    }

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload SendMessagePayload payload, Principal principal) {
        String senderPhoneNumber = principal.getName();

        log.info("Received message from {} to room {}", maskPhone(senderPhoneNumber), payload.roomId());

        try {
            chatService.sendMessage(payload.roomId(), senderPhoneNumber, payload.body());
        } catch (Exception e) {
            log.error("Failed to send message: {}", e.getMessage());
            throw new RuntimeException("Failed to send message: " + e.getMessage());
        }
    }

    public record SendMessagePayload(String roomId, String body) {}

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        return phone.substring(0, 4) + "***";
    }
}
