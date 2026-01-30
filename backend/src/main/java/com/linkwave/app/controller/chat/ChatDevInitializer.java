package com.linkwave.app.controller.chat;

import com.linkwave.app.service.chat.ChatService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Development utility to create a test room on startup.
 * Phase D: Testing helper
 * 
 * Remove or disable in production.
 */
@Component
public class ChatDevInitializer {
    
    private final ChatService chatService;
    
    public ChatDevInitializer(ChatService chatService) {
        this.chatService = chatService;
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void initTestData() {
        try {
            // Create a test room for demo purposes
            // Users can test with phone numbers they've authenticated with
            System.out.println("=".repeat(80));
            System.out.println("Phase D: Real-time Messaging System Ready");
            System.out.println("=".repeat(80));
            System.out.println("To test:");
            System.out.println("1. Login with two different phone numbers at http://localhost:3000");
            System.out.println("2. Create a room using POST /api/v1/chat/rooms/direct");
            System.out.println("3. Navigate to http://localhost:3000/chat");
            System.out.println("4. Send messages in real-time!");
            System.out.println("=".repeat(80));
        } catch (Exception e) {
            System.err.println("Failed to initialize test data: " + e.getMessage());
        }
    }
}
