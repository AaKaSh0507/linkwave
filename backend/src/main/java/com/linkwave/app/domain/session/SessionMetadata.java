package com.linkwave.app.domain.session;

import java.time.Instant;

/**
 * Metadata for a user session stored in Redis.
 * Contains session identification, timestamps, and associated user data.
 */
public class SessionMetadata {

    private final String sessionId;
    private final Instant createdAt;
    private final Instant expiresAt;
    private String phoneNumber;

    public SessionMetadata(String sessionId, Instant createdAt, Instant expiresAt) {
        this.sessionId = sessionId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public SessionMetadata(String sessionId, Instant createdAt, Instant expiresAt, String phoneNumber) {
        this.sessionId = sessionId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.phoneNumber = phoneNumber;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
