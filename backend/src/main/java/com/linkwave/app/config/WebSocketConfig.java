package com.linkwave.app.config;

import com.linkwave.app.websocket.WsAuthenticationInterceptor;
import com.linkwave.app.websocket.WsMessageHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration for realtime messaging.
 * 
 * Endpoint: /ws
 * Protocol: Custom JSON envelope messages
 * Transport: ws:// (dev) and wss:// (production)
 * Auth: Session-based (must be authenticated from Phase B OTP login)
 * 
 * Phase C1: Direct WebSocket gateway
 * - Session-based authentication via interceptor
 * - Custom application protocol (event-based JSON envelopes)
 * - In-memory session management
 * - No Kafka or persistence yet
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    private final WsMessageHandler messageHandler;
    private final WsAuthenticationInterceptor authInterceptor;
    
    public WebSocketConfig(WsMessageHandler messageHandler, 
                          WsAuthenticationInterceptor authInterceptor) {
        this.messageHandler = messageHandler;
        this.authInterceptor = authInterceptor;
    }
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(messageHandler, "/ws")
                .addInterceptors(authInterceptor)
                .setAllowedOrigins("*"); // TODO: Configure allowed origins for production
    }
}
