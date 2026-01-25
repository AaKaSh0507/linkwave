package com.linkwave.app.controller.auth;

import com.linkwave.app.domain.auth.OtpRequestPayload;
import com.linkwave.app.service.auth.OtpService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for authentication operations.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Validated
public class AuthController {

    private final OtpService otpService;

    public AuthController(OtpService otpService) {
        this.otpService = otpService;
    }

    /**
     * Request OTP generation for a phone number.
     * 
     * @param payload the request payload containing phone number
     * @return success response without OTP value
     */
    @PostMapping("/request-otp")
    public ResponseEntity<Map<String, String>> requestOtp(
            @Valid @RequestBody OtpRequestPayload payload) {
        
        otpService.requestOtp(payload.getPhoneNumber());
        
        return ResponseEntity.ok(Map.of(
            "message", "OTP sent successfully"
        ));
    }

    /**
     * Exception handler for throttle violations.
     */
    @ExceptionHandler(OtpService.OtpThrottleException.class)
    public ResponseEntity<Map<String, String>> handleThrottleException(
            OtpService.OtpThrottleException ex) {
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(Map.of("error", ex.getMessage()));
    }
}
