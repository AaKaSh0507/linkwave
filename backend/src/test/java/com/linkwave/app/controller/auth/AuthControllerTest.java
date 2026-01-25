package com.linkwave.app.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkwave.app.config.auth.AuthConfig;
import com.linkwave.app.domain.auth.OtpRequestPayload;
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
    void requestOtp_withValidPhoneNumber_shouldReturnOk() throws Exception {
        OtpRequestPayload payload = new OtpRequestPayload("+1234567890");
        
        doNothing().when(otpService).requestOtp(anyString());

        mockMvc.perform(post("/api/v1/auth/request-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP sent successfully"));

        verify(otpService, times(1)).requestOtp("+1234567890");
    }

    @Test
    void requestOtp_withInvalidPhoneNumber_shouldReturnBadRequest() throws Exception {
        OtpRequestPayload payload = new OtpRequestPayload("invalid");

        mockMvc.perform(post("/api/v1/auth/request-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());

        verify(otpService, never()).requestOtp(anyString());
    }

    @Test
    void requestOtp_withEmptyPhoneNumber_shouldReturnBadRequest() throws Exception {
        OtpRequestPayload payload = new OtpRequestPayload("");

        mockMvc.perform(post("/api/v1/auth/request-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());

        verify(otpService, never()).requestOtp(anyString());
    }

    @Test
    void requestOtp_withNullPhoneNumber_shouldReturnBadRequest() throws Exception {
        OtpRequestPayload payload = new OtpRequestPayload(null);

        mockMvc.perform(post("/api/v1/auth/request-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());

        verify(otpService, never()).requestOtp(anyString());
    }

    @Test
    void requestOtp_whenThrottleLimitExceeded_shouldReturnTooManyRequests() throws Exception {
        OtpRequestPayload payload = new OtpRequestPayload("+1234567890");
        
        doThrow(new OtpService.OtpThrottleException("Too many OTP requests. Please try again later."))
            .when(otpService).requestOtp(anyString());

        mockMvc.perform(post("/api/v1/auth/request-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Too many OTP requests. Please try again later."));

        verify(otpService, times(1)).requestOtp("+1234567890");
    }
}
