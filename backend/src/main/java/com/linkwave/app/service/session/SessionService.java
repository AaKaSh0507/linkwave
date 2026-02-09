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

    public SessionMetadata createSessionFor(String phoneNumber) {
        HttpSession session = getCurrentSession(true);

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(getSessionTimeoutMinutes() * 60L);

        SessionMetadata metadata = new SessionMetadata(
            session.getId(),
            now,
            expiresAt
        );

        return metadata;
    }

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

    public void setSessionAttribute(String key, Object value) {
        HttpSession session = getCurrentSession(true);
        session.setAttribute(key, value);
    }

    public Optional<Object> getSessionAttribute(String key) {
        HttpSession session = getCurrentSession(false);

        if (session == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(session.getAttribute(key));
    }

    public void invalidateSession() {
        HttpSession session = getCurrentSession(false);

        if (session != null) {
            session.invalidate();
        }
    }

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

    public boolean isAuthenticated() {
        return getSessionAttribute("authenticated")
            .filter(attr -> attr instanceof Boolean)
            .map(attr -> (Boolean) attr)
            .orElse(false);
    }

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
