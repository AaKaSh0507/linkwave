package com.linkwave.app.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkwave.app.domain.auth.AuthenticatedUserContext;
import com.linkwave.app.domain.auth.OtpRequestPayload;
import com.linkwave.app.domain.auth.VerificationRequestPayload;
import com.linkwave.app.service.auth.EmailService;
import com.linkwave.app.service.auth.OtpService;
import com.linkwave.app.service.session.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OtpService otpService;

    @MockitoBean
    private SessionService sessionService;

    @Test
    void requestOtp_withValidPayload_shouldReturnOk() throws Exception {
        OtpRequestPayload payload = new OtpRequestPayload("+1234567890", "user@example.com");
        
        doNothing().when(otpService).requestOtp(anyString(), anyString());

        mockMvc.perform(post("/api/v1/auth/request-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP sent successfully"));

        verify(otpService, times(1)).requestOtp("+1234567890", "user@example.com");
    }

    @Test
    void requestOtp_withInvalidPhoneNumber_shouldReturnBadRequest() throws Exception {
        OtpRequestPayload payload = new OtpRequestPayload("invalid", "user@example.com");

        mockMvc.perform(post("/api/v1/auth/request-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());

        verify(otpService, never()).requestOtp(anyString(), anyString());
    }

    @Test
    void requestOtp_withInvalidEmail_shouldReturnBadRequest() throws Exception {
        OtpRequestPayload payload = new OtpRequestPayload("+1234567890", "invalid-email");

        mockMvc.perform(post("/api/v1/auth/request-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());

        verify(otpService, never()).requestOtp(anyString(), anyString());
    }

    @Test
    void requestOtp_withEmptyPhoneNumber_shouldReturnBadRequest() throws Exception {
        OtpRequestPayload payload = new OtpRequestPayload("", "user@example.com");

        mockMvc.perform(post("/api/v1/auth/request-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());

        verify(otpService, never()).requestOtp(anyString(), anyString());
    }

    @Test
    void requestOtp_withNullPhoneNumber_shouldReturnBadRequest() throws Exception {
        OtpRequestPayload payload = new OtpRequestPayload(null, "user@example.com");

        mockMvc.perform(post("/api/v1/auth/request-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());

        verify(otpService, never()).requestOtp(anyString(), anyString());
    }

    @Test
    void requestOtp_whenThrottleLimitExceeded_shouldReturnTooManyRequests() throws Exception {
        OtpRequestPayload payload = new OtpRequestPayload("+1234567890", "user@example.com");
        
        doThrow(new OtpService.OtpThrottleException("Too many OTP requests. Please try again later."))
            .when(otpService).requestOtp(anyString(), anyString());

        mockMvc.perform(post("/api/v1/auth/request-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Too many OTP requests. Please try again later."));

        verify(otpService, times(1)).requestOtp("+1234567890", "user@example.com");
    }

    @Test
    void requestOtp_whenEmailDeliveryFails_shouldReturnInternalServerError() throws Exception {
        OtpRequestPayload payload = new OtpRequestPayload("+1234567890", "user@example.com");
        
        doThrow(new EmailService.EmailDeliveryException("Failed to send OTP email", new RuntimeException()))
            .when(otpService).requestOtp(anyString(), anyString());

        mockMvc.perform(post("/api/v1/auth/request-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Failed to send OTP. Please try again later."));

        verify(otpService, times(1)).requestOtp("+1234567890", "user@example.com");
    }

    // ===== Verification Endpoint Tests =====

    @Test
    void verifyOtp_withValidOtp_shouldReturnOkAndAuthenticateSession() throws Exception {
        VerificationRequestPayload payload = new VerificationRequestPayload("+1234567890", "123456");
        
        when(otpService.verifyOtp(anyString(), anyString())).thenReturn(true);
        when(sessionService.authenticateSession(anyString()))
            .thenReturn(new AuthenticatedUserContext("+1234567890", Instant.now()));

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.message").value("Authentication successful"));

        verify(otpService, times(1)).verifyOtp("+1234567890", "123456");
        verify(sessionService, times(1)).authenticateSession("+1234567890");
    }

    @Test
    void verifyOtp_withIncorrectOtp_shouldReturnUnauthorized() throws Exception {
        VerificationRequestPayload payload = new VerificationRequestPayload("+1234567890", "123456");
        
        doThrow(new OtpService.OtpVerificationException("Invalid OTP"))
            .when(otpService).verifyOtp(anyString(), anyString());

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid OTP"));

        verify(otpService, times(1)).verifyOtp("+1234567890", "123456");
        verify(sessionService, never()).authenticateSession(anyString());
    }

    @Test
    void verifyOtp_withExpiredOtp_shouldReturnUnauthorized() throws Exception {
        VerificationRequestPayload payload = new VerificationRequestPayload("+1234567890", "123456");
        
        doThrow(new OtpService.OtpVerificationException("OTP not found or has expired"))
            .when(otpService).verifyOtp(anyString(), anyString());

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("OTP not found or has expired"));

        verify(otpService, times(1)).verifyOtp("+1234567890", "123456");
        verify(sessionService, never()).authenticateSession(anyString());
    }

    @Test
    void verifyOtp_withInvalidPhoneNumber_shouldReturnBadRequest() throws Exception {
        VerificationRequestPayload payload = new VerificationRequestPayload("invalid", "123456");

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());

        verify(otpService, never()).verifyOtp(anyString(), anyString());
        verify(sessionService, never()).authenticateSession(anyString());
    }

    @Test
    void verifyOtp_withInvalidOtpFormat_shouldReturnBadRequest() throws Exception {
        VerificationRequestPayload payload = new VerificationRequestPayload("+1234567890", "abc");

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());

        verify(otpService, never()).verifyOtp(anyString(), anyString());
        verify(sessionService, never()).authenticateSession(anyString());
    }

    @Test
    void verifyOtp_withNullPhoneNumber_shouldReturnBadRequest() throws Exception {
        VerificationRequestPayload payload = new VerificationRequestPayload(null, "123456");

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());

        verify(otpService, never()).verifyOtp(anyString(), anyString());
        verify(sessionService, never()).authenticateSession(anyString());
    }

    @Test
    void verifyOtp_withNullOtp_shouldReturnBadRequest() throws Exception {
        VerificationRequestPayload payload = new VerificationRequestPayload("+1234567890", null);

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());

        verify(otpService, never()).verifyOtp(anyString(), anyString());
        verify(sessionService, never()).authenticateSession(anyString());
    }
}
