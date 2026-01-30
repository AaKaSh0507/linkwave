package com.linkwave.app.config;

import com.linkwave.app.websocket.StompSessionAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for realtime messaging with STOMP.
 * 
 * Phase D: STOMP over WebSocket
 * 
 * Endpoint: /ws/chat
 * Protocol: STOMP (Simple Text Oriented Messaging Protocol)
 * Transport: ws:// (dev) and wss:// (production)
 * Auth: Session-based (must be authenticated from Phase B OTP login)
 * 
 * Architecture:
 * - Client sends message to /app/chat.send
 * - Server processes and publishes to Kafka
 * - Kafka consumer pushes to /topic/room.{roomId}
 * - All room members receive the message
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    private final StompSessionAuthInterceptor authInterceptor;
    
    public WebSocketConfig(StompSessionAuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat")
                .setAllowedOrigins("http://localhost:3000") // Allow frontend origin for cookie credentials
                .withSockJS(); // Fallback for browsers without WebSocket support
    }
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable simple in-memory broker for /topic and /queue destinations
        registry.enableSimpleBroker("/topic", "/queue");
        
        // Messages to /app will be routed to @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");
        
        // User-specific messages will be sent to /user
        registry.setUserDestinationPrefix("/user");
    }
    
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Add authentication interceptor to validate session before processing messages
        registration.interceptors(authInterceptor);
    }
}
