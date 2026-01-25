package com.linkwave.app.service.session;

import com.linkwave.app.config.RedisConfig;
import com.linkwave.app.domain.session.SessionMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private RedisConfig redisConfig;

    private SessionService sessionService;
    private MockHttpServletRequest request;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        sessionService = new SessionService(redisConfig);
        
        // Setup mock request and session
        request = new MockHttpServletRequest();
        session = new MockHttpSession();
        request.setSession(session);
        
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void createSessionFor_shouldCreateNewSession() {
        when(redisConfig.getSessionTimeoutMinutes()).thenReturn(30);
        
        SessionMetadata metadata = sessionService.createSessionFor("+1234567890");
        
        assertNotNull(metadata);
        assertNotNull(metadata.getSessionId());
        assertNotNull(metadata.getCreatedAt());
        assertNotNull(metadata.getExpiresAt());
        assertFalse(metadata.isExpired());
    }

    @Test
    void getCurrentSessionMetadata_shouldReturnEmptyWhenNoSession() {
        request.setSession(null);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        
        Optional<SessionMetadata> metadata = sessionService.getCurrentSessionMetadata();
        
        assertTrue(metadata.isEmpty());
    }

    @Test
    void getCurrentSessionMetadata_shouldReturnMetadataWhenSessionExists() {
        when(redisConfig.getSessionTimeoutMinutes()).thenReturn(30);
        sessionService.createSessionFor("+1234567890");
        
        Optional<SessionMetadata> metadata = sessionService.getCurrentSessionMetadata();
        
        assertTrue(metadata.isPresent());
        assertNotNull(metadata.get().getSessionId());
    }

    @Test
    void setSessionAttribute_shouldStoreAttribute() {
        sessionService.setSessionAttribute("testKey", "testValue");
        
        Optional<Object> value = sessionService.getSessionAttribute("testKey");
        
        assertTrue(value.isPresent());
        assertEquals("testValue", value.get());
    }

    @Test
    void getSessionAttribute_shouldReturnEmptyWhenNoSession() {
        request.setSession(null);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        
        Optional<Object> value = sessionService.getSessionAttribute("testKey");
        
        assertTrue(value.isEmpty());
    }

    @Test
    void getSessionAttribute_shouldReturnEmptyWhenAttributeNotFound() {
        Optional<Object> value = sessionService.getSessionAttribute("nonExistent");
        
        assertTrue(value.isEmpty());
    }

    @Test
    void invalidateSession_shouldInvalidateCurrentSession() {
        when(redisConfig.getSessionTimeoutMinutes()).thenReturn(30);
        sessionService.createSessionFor("+1234567890");
        
        sessionService.invalidateSession();
        
        assertTrue(session.isInvalid());
    }

    @Test
    void invalidateSession_shouldHandleNoSession() {
        request.setSession(null);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        
        assertDoesNotThrow(() -> sessionService.invalidateSession());
    }
}
