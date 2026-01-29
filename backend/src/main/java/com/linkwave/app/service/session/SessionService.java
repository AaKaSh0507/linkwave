package com.linkwave.app.service.session;

import com.linkwave.app.config.RedisConfig;
import com.linkwave.app.domain.auth.AuthenticatedUserContext;
import com.linkwave.app.domain.session.SessionMetadata;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);
    
    private static final int DEFAULT_SESSION_TIMEOUT_MINUTES = 30;

    private final RedisConfig redisConfig;

    @Autowired
    public SessionService(@Autowired(required = false) RedisConfig redisConfig) {
        this.redisConfig = redisConfig;
    }
    
    private int getSessionTimeoutMinutes() {
        return redisConfig != null ? redisConfig.getSessionTimeoutMinutes() : DEFAULT_SESSION_TIMEOUT_MINUTES;
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
        Instant expiresAt = now.plusSeconds(getSessionTimeoutMinutes() * 60L);
        
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
     * Authenticate the current session with user identity.
     * Stores authenticated flag, phone number, and timestamp in session.
     * 
     * @param phoneNumber the verified phone number
     * @return authenticated user context
     */
    public AuthenticatedUserContext authenticateSession(String phoneNumber) {
        HttpSession session = getCurrentSession(true);
        
        Instant now = Instant.now();
        
        session.setAttribute("authenticated", true);
        session.setAttribute("phoneNumber", phoneNumber);
        session.setAttribute("authenticatedAt", now.toEpochMilli());
        
        AuthenticatedUserContext context = new AuthenticatedUserContext(phoneNumber, now);
        
        log.info("Session authenticated for phone: {}", context.getMaskedPhoneNumber());
        
        return context;
    }

    /**
     * Check if current session is authenticated.
     * 
     * @return true if session has authenticated flag set
     */
    public boolean isAuthenticated() {
        return getSessionAttribute("authenticated")
            .filter(attr -> attr instanceof Boolean)
            .map(attr -> (Boolean) attr)
            .orElse(false);
    }

    /**
     * Get authenticated user context from session.
     * 
     * @return optional authenticated user context
     */
    public Optional<AuthenticatedUserContext> getAuthenticatedUser() {
        if (!isAuthenticated()) {
            return Optional.empty();
        }
        
        Optional<Object> phoneNumberOpt = getSessionAttribute("phoneNumber");
        Optional<Object> authenticatedAtOpt = getSessionAttribute("authenticatedAt");
        
        if (phoneNumberOpt.isEmpty() || authenticatedAtOpt.isEmpty()) {
            return Optional.empty();
        }
        
        String phoneNumber = (String) phoneNumberOpt.get();
        Instant authenticatedAt = Instant.ofEpochMilli((Long) authenticatedAtOpt.get());
        
        return Optional.of(new AuthenticatedUserContext(phoneNumber, authenticatedAt));
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
