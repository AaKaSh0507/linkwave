package com.linkwave.app.controller.auth;

import com.linkwave.app.domain.auth.OtpRequestPayload;
import com.linkwave.app.service.auth.EmailService;
import com.linkwave.app.service.auth.OtpService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final OtpService otpService;

    public AuthController(OtpService otpService) {
        this.otpService = otpService;
    }

    /**
     * Request OTP generation for a phone number and send via email.
     * 
     * @param payload the request payload containing phone number and email
     * @return success response without OTP value
     */
    @PostMapping("/request-otp")
    public ResponseEntity<Map<String, String>> requestOtp(
            @Valid @RequestBody OtpRequestPayload payload) {
        
        otpService.requestOtp(payload.getPhoneNumber(), payload.getEmail());
        
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

    /**
     * Exception handler for email delivery failures.
     */
    @ExceptionHandler(EmailService.EmailDeliveryException.class)
    public ResponseEntity<Map<String, String>> handleEmailDeliveryException(
            EmailService.EmailDeliveryException ex) {
        
        log.error("Email delivery failed: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "Failed to send OTP. Please try again later."));
    }
}
