package com.linkwave.app.domain.user;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * User profile payload for authenticated users.
 * Contains masked phone number and authentication timestamp.
 * No database persistence - purely session-based identity.
 */
public class UserProfilePayload {

    @JsonProperty("phoneNumber")
    private final String maskedPhoneNumber;

    @JsonProperty("authenticatedAt")
    private final String authenticatedAt;

    public UserProfilePayload(String maskedPhoneNumber, String authenticatedAt) {
        this.maskedPhoneNumber = maskedPhoneNumber;
        this.authenticatedAt = authenticatedAt;
    }

    public String getMaskedPhoneNumber() {
        return maskedPhoneNumber;
    }

    public String getAuthenticatedAt() {
        return authenticatedAt;
    }
}
