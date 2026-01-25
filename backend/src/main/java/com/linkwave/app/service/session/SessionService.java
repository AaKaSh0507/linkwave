package com.linkwave.app.service.session;

import com.linkwave.app.config.RedisConfig;
import com.linkwave.app.domain.session.SessionMetadata;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Optional;

/**
 * Service for managing HTTP sessions stored in Redis.
 * Provides abstraction over Spring Session for session lifecycle management.
 */
@Service
public class SessionService {

    private final RedisConfig redisConfig;

    public SessionService(RedisConfig redisConfig) {
        this.redisConfig = redisConfig;
    }

    /**
     * Create a new session for a phone number (future hook for post-OTP verification).
     * In B3, this creates an empty session. In B4+, this will store phoneNumber.
     * 
     * @param phoneNumber the phone number to associate with session (not stored in B3)
     * @return session metadata
     */
    public SessionMetadata createSessionFor(String phoneNumber) {
        HttpSession session = getCurrentSession(true);
        
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(redisConfig.getSessionTimeoutMinutes() * 60L);
        
        SessionMetadata metadata = new SessionMetadata(
            session.getId(),
            now,
            expiresAt
        );
        
        // Note: phoneNumber will be stored in session after OTP verification (B4+)
        // For now, session remains empty but validated
        
        return metadata;
    }

    /**
     * Get current session metadata if exists.
     * 
     * @return optional session metadata
     */
    public Optional<SessionMetadata> getCurrentSessionMetadata() {
        HttpSession session = getCurrentSession(false);
        
        if (session == null) {
            return Optional.empty();
        }
        
        Instant createdAt = Instant.ofEpochMilli(session.getCreationTime());
        Instant expiresAt = Instant.ofEpochMilli(session.getLastAccessedTime())
            .plusSeconds(session.getMaxInactiveInterval());
        
        String phoneNumber = (String) session.getAttribute("phoneNumber");
        
        SessionMetadata metadata = new SessionMetadata(
            session.getId(),
            createdAt,
            expiresAt,
            phoneNumber
        );
        
        return Optional.of(metadata);
    }

    /**
     * Set an attribute in the current session.
     * 
     * @param key attribute key
     * @param value attribute value
     */
    public void setSessionAttribute(String key, Object value) {
        HttpSession session = getCurrentSession(true);
        session.setAttribute(key, value);
    }

    /**
     * Get an attribute from the current session.
     * 
     * @param key attribute key
     * @return optional attribute value
     */
    public Optional<Object> getSessionAttribute(String key) {
        HttpSession session = getCurrentSession(false);
        
        if (session == null) {
            return Optional.empty();
        }
        
        return Optional.ofNullable(session.getAttribute(key));
    }

    /**
     * Invalidate the current session.
     */
    public void invalidateSession() {
        HttpSession session = getCurrentSession(false);
        
        if (session != null) {
            session.invalidate();
        }
    }

    /**
     * Get current HTTP session from request context.
     * 
     * @param create whether to create new session if none exists
     * @return HTTP session or null
     */
    private HttpSession getCurrentSession(boolean create) {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        
        if (attributes == null) {
            return null;
        }
        
        HttpServletRequest request = attributes.getRequest();
        return request.getSession(create);
    }
}
