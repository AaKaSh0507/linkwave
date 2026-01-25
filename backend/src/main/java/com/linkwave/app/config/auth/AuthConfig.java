package com.linkwave.app.config.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for OTP authentication.
 */
@Configuration
@ConfigurationProperties(prefix = "linkwave.auth.otp")
public class AuthConfig {

    /**
     * Length of the generated OTP code.
     */
    private int otpLength = 6;

    /**
     * Time-to-live for OTP in seconds.
     */
    private int ttlSeconds = 300; // 5 minutes

    /**
     * Maximum number of OTP requests allowed per phone number within the throttle window.
     */
    private int throttleMaxRequests = 3;

    /**
     * Throttle time window in seconds.
     */
    private int throttleWindowSeconds = 600; // 10 minutes

    public int getOtpLength() {
        return otpLength;
    }

    public void setOtpLength(int otpLength) {
        this.otpLength = otpLength;
    }

    public int getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(int ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public int getThrottleMaxRequests() {
        return throttleMaxRequests;
    }

    public void setThrottleMaxRequests(int throttleMaxRequests) {
        this.throttleMaxRequests = throttleMaxRequests;
    }

    public int getThrottleWindowSeconds() {
        return throttleWindowSeconds;
    }

    public void setThrottleWindowSeconds(int throttleWindowSeconds) {
        this.throttleWindowSeconds = throttleWindowSeconds;
    }
}
