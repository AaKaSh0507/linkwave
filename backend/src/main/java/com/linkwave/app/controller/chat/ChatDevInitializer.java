package com.linkwave.app.controller.chat;

import com.linkwave.app.service.chat.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Development initializer that logs startup information for the chat system.
 * This component displays helpful information when the application starts.
 */
@Component
public class ChatDevInitializer {

    private static final Logger logger = LoggerFactory.getLogger(ChatDevInitializer.class);
    private final ChatService chatService;

    public ChatDevInitializer(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Logs startup information when the application is ready.
     * Displays testing instructions for the real-time messaging system.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initTestData() {
        try {
            logger.info("=".repeat(80));
            logger.info("Phase D: Real-time Messaging System Ready");
            logger.info("=".repeat(80));
            logger.info("To test:");
            logger.info("1. Login with two different phone numbers at http://localhost:3000");
            logger.info("2. Create a room using POST /api/v1/chat/rooms/direct");
            logger.info("3. Navigate to http://localhost:3000/chat");
            logger.info("4. Send messages in real-time!");
            logger.info("=".repeat(80));
        } catch (Exception e) {
            logger.error("Failed to initialize test data: {}", e.getMessage(), e);
        }
    }
}
