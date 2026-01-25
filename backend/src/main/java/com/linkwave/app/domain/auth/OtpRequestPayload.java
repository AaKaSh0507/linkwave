package com.linkwave.app.domain.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request payload for OTP generation.
 */
public class OtpRequestPayload {
    
    @NotBlank(message = "Phone number is required")
    @Pattern(
        regexp = "^\\+?[1-9]\\d{1,14}$",
        message = "Phone number must be a valid E.164 format"
    )
    private String phoneNumber;

    public OtpRequestPayload() {
    }

    public OtpRequestPayload(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
