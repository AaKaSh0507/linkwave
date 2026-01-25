package com.linkwave.app.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkwave.app.domain.auth.OtpRequestPayload;
import com.linkwave.app.service.auth.EmailService;
import com.linkwave.app.service.auth.OtpService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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

    @MockBean
    private OtpService otpService;

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
}
