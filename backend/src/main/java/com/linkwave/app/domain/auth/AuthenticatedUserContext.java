package com.linkwave.app.domain.auth;

import java.time.Instant;

/**
 * Context object representing an authenticated user in the session.
 * Contains minimal identity information (phone number) and authentication timestamp.
 * Not persisted to database - used only for session scope.
 */
public class AuthenticatedUserContext {
    
    private final String phoneNumber;
    private final Instant authenticatedAt;

    public AuthenticatedUserContext(String phoneNumber, Instant authenticatedAt) {
        this.phoneNumber = phoneNumber;
        this.authenticatedAt = authenticatedAt;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public Instant getAuthenticatedAt() {
        return authenticatedAt;
    }

    /**
     * Mask phone number for logging (show first 4 chars and last 2).
     */
    public String getMaskedPhoneNumber() {
        if (phoneNumber == null || phoneNumber.length() < 7) {
            return "***";
        }
        return phoneNumber.substring(0, 4) + "***" + phoneNumber.substring(phoneNumber.length() - 2);
    }
}
