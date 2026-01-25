package com.linkwave.app.service.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for WebSocket session manager.
 */
class WsSessionManagerTest {
    
    private WsSessionManager sessionManager;
    
    @BeforeEach
    void setUp() {
        sessionManager = new WsSessionManager();
    }
    
    @Test
    void registerSession_shouldStoreMapping() {
        // Given
        String phoneNumber = "+14155552671";
        WebSocketSession session = mockSession("session-1", true);
        
        // When
        sessionManager.registerSession(phoneNumber, session);
        
        // Then
        assertThat(sessionManager.hasActiveSession(phoneNumber)).isTrue();
        assertThat(sessionManager.getSession(phoneNumber)).isPresent();
        assertThat(sessionManager.getPhoneNumber(session)).contains(phoneNumber);
        assertThat(sessionManager.getActiveSessionCount()).isEqualTo(1);
    }
    
    @Test
    void registerSession_withExistingSession_shouldReplaceOldSession() {
        // Given: First session
        String phoneNumber = "+14155552671";
        WebSocketSession oldSession = mockSession("session-1", true);
        sessionManager.registerSession(phoneNumber, oldSession);
        
        // When: Second session for same user
        WebSocketSession newSession = mockSession("session-2", true);
        sessionManager.registerSession(phoneNumber, newSession);
        
        // Then: New session should replace old one
        assertThat(sessionManager.getSession(phoneNumber)).contains(newSession);
        assertThat(sessionManager.getPhoneNumber(newSession)).contains(phoneNumber);
        assertThat(sessionManager.getActiveSessionCount()).isEqualTo(1);
    }
    
    @Test
    void deregisterSession_shouldRemoveMapping() {
        // Given
        String phoneNumber = "+14155552671";
        WebSocketSession session = mockSession("session-1", true);
        sessionManager.registerSession(phoneNumber, session);
        
        // When
        sessionManager.deregisterSession(session);
        
        // Then
        assertThat(sessionManager.hasActiveSession(phoneNumber)).isFalse();
        assertThat(sessionManager.getSession(phoneNumber)).isEmpty();
        assertThat(sessionManager.getPhoneNumber(session)).isEmpty();
        assertThat(sessionManager.getActiveSessionCount()).isEqualTo(0);
    }
    
    @Test
    void getSession_withClosedSession_shouldReturnEmpty() {
        // Given: Closed session
        String phoneNumber = "+14155552671";
        WebSocketSession session = mockSession("session-1", false);
        sessionManager.registerSession(phoneNumber, session);
        
        // When
        var result = sessionManager.getSession(phoneNumber);
        
        // Then: Should return empty and clean up
        assertThat(result).isEmpty();
        assertThat(sessionManager.getActiveSessionCount()).isEqualTo(0);
    }
    
    @Test
    void multipleSessions_shouldTrackIndependently() {
        // Given: Multiple users with sessions
        String phone1 = "+14155552671";
        String phone2 = "+14155552672";
        String phone3 = "+14155552673";
        
        WebSocketSession session1 = mockSession("session-1", true);
        WebSocketSession session2 = mockSession("session-2", true);
        WebSocketSession session3 = mockSession("session-3", true);
        
        // When
        sessionManager.registerSession(phone1, session1);
        sessionManager.registerSession(phone2, session2);
        sessionManager.registerSession(phone3, session3);
        
        // Then
        assertThat(sessionManager.getActiveSessionCount()).isEqualTo(3);
        assertThat(sessionManager.hasActiveSession(phone1)).isTrue();
        assertThat(sessionManager.hasActiveSession(phone2)).isTrue();
        assertThat(sessionManager.hasActiveSession(phone3)).isTrue();
        
        // When: Deregister one
        sessionManager.deregisterSession(session2);
        
        // Then
        assertThat(sessionManager.getActiveSessionCount()).isEqualTo(2);
        assertThat(sessionManager.hasActiveSession(phone1)).isTrue();
        assertThat(sessionManager.hasActiveSession(phone2)).isFalse();
        assertThat(sessionManager.hasActiveSession(phone3)).isTrue();
    }
    
    @Test
    void getPhoneNumber_withUnknownSession_shouldReturnEmpty() {
        // Given: Unknown session
        WebSocketSession session = mockSession("unknown", true);
        
        // When
        var result = sessionManager.getPhoneNumber(session);
        
        // Then
        assertThat(result).isEmpty();
    }
    
    private WebSocketSession mockSession(String id, boolean isOpen) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(isOpen);
        return session;
    }
}
