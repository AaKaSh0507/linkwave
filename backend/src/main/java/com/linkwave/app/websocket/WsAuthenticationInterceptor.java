package com.linkwave.app.websocket;

import com.linkwave.app.domain.auth.AuthenticatedUserContext;
import com.linkwave.app.service.session.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket handshake interceptor for session-based authentication.
 * 
 * Validates that the user has an authenticated HTTP session before
 * allowing WebSocket upgrade. If not authenticated, rejects the
 * connection with 1008 (policy violation).
 * 
 * Security:
 * - WebSocket upgrade inherits session cookie from HTTP
 * - CSRF not required for WebSocket (connection-based protocol)
 * - Phone number is stored in WebSocket session attributes for use in handler
 */
@Component
public class WsAuthenticationInterceptor implements HandshakeInterceptor {
    
    private static final Logger log = LoggerFactory.getLogger(WsAuthenticationInterceptor.class);
    
    private final SessionService sessionService;
    
    public WsAuthenticationInterceptor(SessionService sessionService) {
        this.sessionService = sessionService;
    }
    
    /**
     * Before handshake: Check if user is authenticated.
     * Reject connection if not authenticated.
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, 
                                  ServerHttpResponse response, 
                                  WebSocketHandler wsHandler, 
                                  Map<String, Object> attributes) throws Exception {
        
        // Get authenticated user from session
        var authenticatedUser = sessionService.getAuthenticatedUser();
        
        if (authenticatedUser.isEmpty()) {
            log.warn("WebSocket handshake rejected: Not authenticated (remote: {})", 
                     request.getRemoteAddress());
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false; // Reject handshake
        }
        
        AuthenticatedUserContext userContext = authenticatedUser.get();
        String phoneNumber = userContext.getPhoneNumber();
        
        // Store phone number in WebSocket session attributes for handler access
        attributes.put("phoneNumber", phoneNumber);
        attributes.put("authenticatedAt", userContext.getAuthenticatedAt());
        
        log.info("WebSocket handshake accepted for user: {} (remote: {})", 
                 userContext.getMaskedPhoneNumber(), request.getRemoteAddress());
        
        return true; // Allow handshake
    }
    
    /**
     * After handshake: Connection established.
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, 
                              ServerHttpResponse response, 
                              WebSocketHandler wsHandler, 
                              Exception exception) {
        // No action needed after handshake
        if (exception != null) {
            log.error("WebSocket handshake error: {}", exception.getMessage());
        }
    }
}
