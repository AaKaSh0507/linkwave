package com.linkwave.app.domain.auth;

import java.time.Instant;

/**
 * Metadata for a generated OTP including value, creation time, and expiration.
 */
public class OtpMetadata {
    
    private final String otpValue;
    private final Instant createdAt;
    private final Instant expiresAt;

    public OtpMetadata(String otpValue, Instant createdAt, Instant expiresAt) {
        this.otpValue = otpValue;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getOtpValue() {
        return otpValue;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
