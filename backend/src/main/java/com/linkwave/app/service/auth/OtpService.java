package com.linkwave.app.service.auth;

import com.linkwave.app.config.auth.AuthConfig;
import com.linkwave.app.domain.auth.OtpMetadata;
import com.linkwave.app.domain.auth.ThrottleMetadata;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for OTP generation and throttling.
 * Uses in-memory storage as placeholder for future Redis/database integration.
 */
@Service
public class OtpService {

    private final AuthConfig authConfig;
    private final EmailService emailService;
    private final SecureRandom secureRandom;
    
    // In-memory storage (to be replaced with Redis in future)
    private final Map<String, OtpMetadata> otpStore;
    private final Map<String, ThrottleMetadata> throttleStore;

    public OtpService(AuthConfig authConfig, EmailService emailService) {
        this.authConfig = authConfig;
        this.emailService = emailService;
        this.secureRandom = new SecureRandom();
        this.otpStore = new ConcurrentHashMap<>();
        this.throttleStore = new ConcurrentHashMap<>();
    }

    /**
     * Request OTP generation for a phone number and send via email.
     * Validates throttle rules before generating.
     * 
     * @param phoneNumber the phone number to generate OTP for (used for throttling)
     * @param email the email address to send OTP to
     * @throws OtpThrottleException if throttle limit is exceeded
     * @throws EmailService.EmailDeliveryException if email sending fails
     */
    public void requestOtp(String phoneNumber, String email) {
        validateThrottle(phoneNumber);
        
        String otpValue = generateOtp();
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(authConfig.getTtlSeconds());
        
        OtpMetadata otpMetadata = new OtpMetadata(otpValue, now, expiresAt);
        otpStore.put(phoneNumber, otpMetadata);
        
        // Send OTP via email
        emailService.sendOtpEmail(email, otpValue);
        
        // Record request for throttling
        recordRequest(phoneNumber, now);
    }

    /**
     * Generate a random numeric OTP.
     */
    private String generateOtp() {
        int otpLength = authConfig.getOtpLength();
        int bound = (int) Math.pow(10, otpLength);
        int otp = secureRandom.nextInt(bound);
        return String.format("%0" + otpLength + "d", otp);
    }

    /**
     * Validate throttle rules for a phone number.
     * 
     * @throws OtpThrottleException if throttle limit is exceeded
     */
    private void validateThrottle(String phoneNumber) {
        ThrottleMetadata metadata = throttleStore.computeIfAbsent(
            phoneNumber, 
            k -> new ThrottleMetadata()
        );
        
        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds(authConfig.getThrottleWindowSeconds());
        
        // Clean up old requests
        metadata.cleanupOldRequests(windowStart);
        
        int requestCount = metadata.getRequestCountWithinWindow(windowStart);
        
        if (requestCount >= authConfig.getThrottleMaxRequests()) {
            throw new OtpThrottleException(
                "Too many OTP requests. Please try again later."
            );
        }
    }

    /**
     * Record an OTP request for throttling purposes.
     */
    private void recordRequest(String phoneNumber, Instant timestamp) {
        ThrottleMetadata metadata = throttleStore.computeIfAbsent(
            phoneNumber,
            k -> new ThrottleMetadata()
        );
        metadata.addRequest(timestamp);
    }

    /**
     * Exception thrown when OTP throttle limit is exceeded.
     */
    public static class OtpThrottleException extends RuntimeException {
        public OtpThrottleException(String message) {
            super(message);
        }
    }
}
