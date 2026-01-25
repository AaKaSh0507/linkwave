package com.linkwave.app.domain.auth;

/**
 * Response payload for OTP verification.
 */
public class VerificationResponsePayload {
    
    private final boolean authenticated;
    private final String message;

    public VerificationResponsePayload(boolean authenticated, String message) {
        this.authenticated = authenticated;
        this.message = message;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public String getMessage() {
        return message;
    }
}
