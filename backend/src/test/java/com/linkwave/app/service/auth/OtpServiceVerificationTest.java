package com.linkwave.app.service.auth;

import com.linkwave.app.config.auth.AuthConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OTP verification functionality in OtpService.
 */
@ExtendWith(MockitoExtension.class)
class OtpServiceVerificationTest {

    @Mock
    private AuthConfig authConfig;

    @Mock
    private EmailService emailService;

    private OtpService otpService;

    @BeforeEach
    void setUp() {
        lenient().when(authConfig.getOtpLength()).thenReturn(6);
        lenient().when(authConfig.getTtlSeconds()).thenReturn(300); // 5 minutes
        lenient().when(authConfig.getThrottleMaxRequests()).thenReturn(3);
        lenient().when(authConfig.getThrottleWindowSeconds()).thenReturn(600); // 10 minutes

        otpService = new OtpService(authConfig, emailService);
    }

    /**
     * Helper method to capture OTP sent via email.
     */
    private String captureOtpFromEmail(String email) {
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtpEmail(eq(email), otpCaptor.capture());
        return otpCaptor.getValue();
    }

    @Test
    void verifyOtp_withValidOtp_shouldSucceed() {
        // Given: OTP requested
        String phoneNumber = "+14155552671";
        String email = "test@example.com";
        
        doNothing().when(emailService).sendOtpEmail(anyString(), anyString());
        otpService.requestOtp(phoneNumber, email);
        String generatedOtp = captureOtpFromEmail(email);

        // When: OTP verified with correct code
        boolean result = otpService.verifyOtp(phoneNumber, generatedOtp);

        // Then: Should succeed
        assertThat(result).isTrue();
        
        // And OTP should be removed (single-use)
        assertThatThrownBy(() -> otpService.verifyOtp(phoneNumber, generatedOtp))
            .isInstanceOf(OtpService.OtpVerificationException.class)
            .hasMessageContaining("No OTP found");
    }

    @Test
    void verifyOtp_withIncorrectOtp_shouldThrowException() {
        // Given: OTP requested
        String phoneNumber = "+14155552671";
        String email = "test@example.com";
        
        doNothing().when(emailService).sendOtpEmail(anyString(), anyString());
        otpService.requestOtp(phoneNumber, email);
        String generatedOtp = captureOtpFromEmail(email);

        // When/Then: Wrong OTP should fail
        assertThatThrownBy(() -> otpService.verifyOtp(phoneNumber, "000000"))
            .isInstanceOf(OtpService.OtpVerificationException.class)
            .hasMessageContaining("Invalid OTP");

        // Original OTP should still be valid (not removed on incorrect attempt)
        boolean result = otpService.verifyOtp(phoneNumber, generatedOtp);
        assertThat(result).isTrue();
    }

    @Test
    void verifyOtp_withExpiredOtp_shouldThrowException() throws InterruptedException {
        // Given: OTP requested with 1 second TTL
        when(authConfig.getTtlSeconds()).thenReturn(1);
        otpService = new OtpService(authConfig, emailService);
        
        String phoneNumber = "+14155552671";
        String email = "test@example.com";
        
        doNothing().when(emailService).sendOtpEmail(anyString(), anyString());
        otpService.requestOtp(phoneNumber, email);
        String generatedOtp = captureOtpFromEmail(email);

        // When: Wait for OTP to expire
        Thread.sleep(1100); // Wait slightly longer than TTL

        // Then: Verification should fail with expiration message
        assertThatThrownBy(() -> otpService.verifyOtp(phoneNumber, generatedOtp))
            .isInstanceOf(OtpService.OtpVerificationException.class)
            .hasMessageContaining("expired");
    }

    @Test
    void verifyOtp_withNonExistentPhoneNumber_shouldThrowException() {
        // Given: No OTP requested for this phone number
        String phoneNumber = "+14155552671";

        // When/Then: Verification should fail (no need to mock email service, no request made)
        assertThatThrownBy(() -> otpService.verifyOtp(phoneNumber, "123456"))
            .isInstanceOf(OtpService.OtpVerificationException.class)
            .hasMessageContaining("No OTP found");
    }

    @Test
    void verifyOtp_withSingleUseEnforcement_shouldRemoveOtpAfterSuccess() {
        // Given: OTP requested
        String phoneNumber = "+14155552671";
        String email = "test@example.com";
        
        doNothing().when(emailService).sendOtpEmail(anyString(), anyString());
        otpService.requestOtp(phoneNumber, email);
        String generatedOtp = captureOtpFromEmail(email);

        // When: First verification succeeds
        boolean result = otpService.verifyOtp(phoneNumber, generatedOtp);
        assertThat(result).isTrue();

        // Then: Second verification with same OTP should fail (single-use)
        assertThatThrownBy(() -> otpService.verifyOtp(phoneNumber, generatedOtp))
            .isInstanceOf(OtpService.OtpVerificationException.class)
            .hasMessageContaining("No OTP found");
    }

    @Test
    void verifyOtp_withMultipleIncorrectAttempts_shouldNotRemoveOtp() {
        // Given: OTP requested
        String phoneNumber = "+14155552671";
        String email = "test@example.com";
        
        doNothing().when(emailService).sendOtpEmail(anyString(), anyString());
        otpService.requestOtp(phoneNumber, email);
        String generatedOtp = captureOtpFromEmail(email);

        // When: Multiple incorrect attempts
        assertThatThrownBy(() -> otpService.verifyOtp(phoneNumber, "111111"))
            .isInstanceOf(OtpService.OtpVerificationException.class);
        
        assertThatThrownBy(() -> otpService.verifyOtp(phoneNumber, "222222"))
            .isInstanceOf(OtpService.OtpVerificationException.class);

        // Then: Correct OTP should still work
        boolean result = otpService.verifyOtp(phoneNumber, generatedOtp);
        assertThat(result).isTrue();
    }

    @Test
    void verifyOtp_withDifferentPhoneNumbers_shouldNotInterfere() {
        // Given: OTPs requested for two different numbers
        String phoneNumber1 = "+14155552671";
        String phoneNumber2 = "+14155552672";
        String email = "test@example.com";
        
        doNothing().when(emailService).sendOtpEmail(anyString(), anyString());
        otpService.requestOtp(phoneNumber1, email);
        String otp1 = captureOtpFromEmail(email);
        
        reset(emailService);
        doNothing().when(emailService).sendOtpEmail(anyString(), anyString());
        otpService.requestOtp(phoneNumber2, email);
        String otp2 = captureOtpFromEmail(email);

        // When: Verify first number's OTP
        boolean result1 = otpService.verifyOtp(phoneNumber1, otp1);
        assertThat(result1).isTrue();

        // Then: Second number's OTP should still be valid
        boolean result2 = otpService.verifyOtp(phoneNumber2, otp2);
        assertThat(result2).isTrue();
    }
}
