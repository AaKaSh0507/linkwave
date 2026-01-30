package com.linkwave.app.websocket;

import com.linkwave.app.domain.auth.AuthenticatedUserContext;
import com.linkwave.app.service.session.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * STOMP channel interceptor for session-based authentication.
 * 
 * Phase D: STOMP authentication
 * 
 * Validates that the user has an authenticated HTTP session before
 * allowing STOMP connection. Extracts session cookie from handshake
 * and resolves user identity.
 * 
 * Security:
 * - STOMP connection inherits session cookie from HTTP handshake
 * - Phone number is stored as Principal for use in message handlers
 * - Unauthorized connections are rejected
 */
@Component
public class StompSessionAuthInterceptor implements ChannelInterceptor {
    
    private static final Logger log = LoggerFactory.getLogger(StompSessionAuthInterceptor.class);
    
    private final SessionService sessionService;
    
    public StompSessionAuthInterceptor(SessionService sessionService) {
        this.sessionService = sessionService;
    }
    
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Authenticate on CONNECT
            AuthenticatedUserContext user = sessionService.getAuthenticatedUser()
                .orElseThrow(() -> {
                    log.warn("WebSocket CONNECT rejected: No authenticated session");
                    return new SecurityException("Unauthorized - No authenticated session");
                });
            
            // Set user as Principal for downstream message handlers
            accessor.setUser(new Principal() {
                @Override
                public String getName() {
                    return user.getPhoneNumber();
                }
            });
            
            log.info("WebSocket CONNECT accepted for user: {}", maskPhoneNumber(user.getPhoneNumber()));
        }
        
        return message;
    }
    
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 7) {
            return "***";
        }
        return phoneNumber.substring(0, 4) + "***" + phoneNumber.substring(phoneNumber.length() - 2);
    }
}
