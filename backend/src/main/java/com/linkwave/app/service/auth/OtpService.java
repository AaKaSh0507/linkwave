package com.linkwave.app.service.auth;

import com.linkwave.app.config.auth.AuthConfig;
import com.linkwave.app.domain.auth.OtpMetadata;
import com.linkwave.app.domain.auth.ThrottleMetadata;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OtpService {

  private static final Logger log = LoggerFactory.getLogger(OtpService.class);

  private final AuthConfig authConfig;
  private final EmailService emailService;
  private final SecureRandom secureRandom;
  private final Map<String, OtpMetadata> otpStore;
  private final Map<String, ThrottleMetadata> throttleStore;
  private final Counter otpRequestSuccess;
  private final Counter otpRequestFailure;
  private final Counter otpVerifySuccess;
  private final Counter otpVerifyFailure;
  private final Counter otpVerifyExpired;
  private final Timer otpGenerationTimer;

  public OtpService(AuthConfig authConfig, EmailService emailService, MeterRegistry meterRegistry) {
    this.authConfig = authConfig;
    this.emailService = emailService;
    this.secureRandom = new SecureRandom();
    this.otpStore = new ConcurrentHashMap<>();
    this.throttleStore = new ConcurrentHashMap<>();

    this.otpRequestSuccess =
        Counter.builder("otp.requests.total").tag("status", "success").register(meterRegistry);
    this.otpRequestFailure =
        Counter.builder("otp.requests.total").tag("status", "failure").register(meterRegistry);
    this.otpVerifySuccess =
        Counter.builder("otp.verifications.total").tag("result", "success").register(meterRegistry);
    this.otpVerifyFailure =
        Counter.builder("otp.verifications.total").tag("result", "failure").register(meterRegistry);
    this.otpVerifyExpired =
        Counter.builder("otp.verifications.total").tag("result", "expired").register(meterRegistry);
    this.otpGenerationTimer = Timer.builder("otp.generation.duration").register(meterRegistry);
    Gauge.builder("otp.active.count", otpStore, Map::size).register(meterRegistry);
  }

  public void requestOtp(String phoneNumber, String email) {
    try {
      validateThrottle(phoneNumber);

      String otpValue = otpGenerationTimer.record(this::generateOtp);
      Instant now = Instant.now();
      Instant expiresAt = now.plusSeconds(authConfig.getTtlSeconds());
      OtpMetadata otpMetadata = new OtpMetadata(otpValue, now, expiresAt);
      otpStore.put(phoneNumber, otpMetadata);
      emailService.sendOtpEmail(email, otpValue);
      recordRequest(phoneNumber, now);
      otpRequestSuccess.increment();
      log.info("OTP requested: phone={}", maskPhone(phoneNumber));
    } catch (Exception e) {
      otpRequestFailure.increment();
      throw e;
    }
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
      log.warn("OTP throttled: phone={} requests={}", maskPhone(phoneNumber), requestCount);
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
      otpVerifyFailure.increment();
      log.warn("OTP verification failed: phone={} reason=not_found", maskPhone(phoneNumber));
      throw new OtpVerificationException("No OTP found for this phone number");
    }

    if (metadata.isExpired()) {
      otpStore.remove(phoneNumber);
      otpVerifyExpired.increment();
      log.warn("OTP verification failed: phone={} reason=expired", maskPhone(phoneNumber));
      throw new OtpVerificationException("OTP has expired");
    }

    if (!metadata.getOtpValue().equals(otp)) {
      otpVerifyFailure.increment();
      log.warn("OTP verification failed: phone={} reason=invalid", maskPhone(phoneNumber));
      throw new OtpVerificationException("Invalid OTP");
    }

    otpStore.remove(phoneNumber);
    otpVerifySuccess.increment();
    log.info("OTP verified: phone={}", maskPhone(phoneNumber));

    return true;
  }

  private String maskPhone(String phone) {
    if (phone == null || phone.length() < 4) {
      return "***";
    }
    return "***" + phone.substring(phone.length() - 4);
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
