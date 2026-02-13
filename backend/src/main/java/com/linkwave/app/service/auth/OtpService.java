package com.linkwave.app.service.auth;

import com.linkwave.app.config.auth.AuthConfig;
import com.linkwave.app.domain.auth.OtpMetadata;
import com.linkwave.app.domain.auth.ThrottleMetadata;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class OtpService {

  private final AuthConfig authConfig;
  private final EmailService emailService;
  private final SecureRandom secureRandom;
  private final Map<String, OtpMetadata> otpStore;
  private final Map<String, ThrottleMetadata> throttleStore;

  public OtpService(AuthConfig authConfig, EmailService emailService) {
    this.authConfig = authConfig;
    this.emailService = emailService;
    this.secureRandom = new SecureRandom();
    this.otpStore = new ConcurrentHashMap<>();
    this.throttleStore = new ConcurrentHashMap<>();
  }

  public void requestOtp(String phoneNumber, String email) {
    validateThrottle(phoneNumber);

    String otpValue = generateOtp();
    Instant now = Instant.now();
    Instant expiresAt = now.plusSeconds(authConfig.getTtlSeconds());
    OtpMetadata otpMetadata = new OtpMetadata(otpValue, now, expiresAt);
    otpStore.put(phoneNumber, otpMetadata);
    emailService.sendOtpEmail(email, otpValue);
    recordRequest(phoneNumber, now);
  }

  private String generateOtp() {
    int otpLength = authConfig.getOtpLength();
    int bound = (int) Math.pow(10, otpLength);
    int otp = secureRandom.nextInt(bound);
    return String.format("%0" + otpLength + "d", otp);
  }

  private void validateThrottle(String phoneNumber) {
    ThrottleMetadata metadata =
        throttleStore.computeIfAbsent(phoneNumber, k -> new ThrottleMetadata());

    Instant now = Instant.now();
    Instant windowStart = now.minusSeconds(authConfig.getThrottleWindowSeconds());
    metadata.cleanupOldRequests(windowStart);
    int requestCount = metadata.getRequestCountWithinWindow(windowStart);

    if (requestCount >= authConfig.getThrottleMaxRequests()) {
      throw new OtpThrottleException("Too many OTP requests. Please try again later.");
    }
  }

  private void recordRequest(String phoneNumber, Instant timestamp) {
    ThrottleMetadata metadata =
        throttleStore.computeIfAbsent(phoneNumber, k -> new ThrottleMetadata());
    metadata.addRequest(timestamp);
  }

  public boolean verifyOtp(String phoneNumber, String otp) {
    OtpMetadata metadata = otpStore.get(phoneNumber);

    if (metadata == null) {
      throw new OtpVerificationException("No OTP found for this phone number");
    }

    if (metadata.isExpired()) {
      otpStore.remove(phoneNumber);
      throw new OtpVerificationException("OTP has expired");
    }

    if (!metadata.getOtpValue().equals(otp)) {
      throw new OtpVerificationException("Invalid OTP");
    }

    otpStore.remove(phoneNumber);

    return true;
  }

  public static class OtpThrottleException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public OtpThrottleException(String message) {
      super(message);
    }
  }

  public static class OtpVerificationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public OtpVerificationException(String message) {
      super(message);
    }
  }
}
