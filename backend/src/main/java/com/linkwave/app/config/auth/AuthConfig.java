package com.linkwave.app.config.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "linkwave.auth.otp")
public class AuthConfig {

  private int otpLength = 6;
  private int ttlSeconds = 300;
  private int throttleMaxRequests = 3;
  private int throttleWindowSeconds = 600;
  private String fixedValue = "";

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

  public String getFixedValue() {
    return fixedValue;
  }

  public void setFixedValue(String fixedValue) {
    this.fixedValue = fixedValue;
  }
}
