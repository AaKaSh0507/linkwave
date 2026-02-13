package com.linkwave.app.domain.auth;

import java.time.Instant;

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

  public String getMaskedPhoneNumber() {
    if (phoneNumber == null || phoneNumber.length() < 7) {
      return "***";
    }
    return phoneNumber.substring(0, 4) + "***" + phoneNumber.substring(phoneNumber.length() - 2);
  }
}
