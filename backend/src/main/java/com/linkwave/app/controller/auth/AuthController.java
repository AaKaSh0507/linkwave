package com.linkwave.app.controller.auth;

import com.linkwave.app.domain.auth.AuthenticatedUserContext;
import com.linkwave.app.domain.auth.OtpRequestPayload;
import com.linkwave.app.domain.auth.VerificationRequestPayload;
import com.linkwave.app.domain.auth.VerificationResponsePayload;
import com.linkwave.app.service.auth.EmailService;
import com.linkwave.app.service.auth.OtpService;
import com.linkwave.app.service.session.SessionService;
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
    private final SessionService sessionService;

    public AuthController(OtpService otpService, SessionService sessionService) {
        this.otpService = otpService;
        this.sessionService = sessionService;
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
     * Verify OTP and authenticate session.
     * 
     * @param payload the request payload containing phone number and OTP
     * @return verification response with authentication status
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<VerificationResponsePayload> verifyOtp(
            @Valid @RequestBody VerificationRequestPayload payload) {
        
        // Verify OTP
        otpService.verifyOtp(payload.getPhoneNumber(), payload.getOtp());
        
        // Authenticate session
        AuthenticatedUserContext userContext = sessionService.authenticateSession(payload.getPhoneNumber());
        
        log.info("User authenticated: {}", userContext.getMaskedPhoneNumber());
        
        return ResponseEntity.ok(
            new VerificationResponsePayload(true, "Authentication successful")
        );
    }

    /**
     * Get CSRF token for authenticated requests.
     * Token is automatically set in XSRF-TOKEN cookie by Spring Security.
     * 
     * @param token the CSRF token (injected by Spring)
     * @return CSRF token value
     */
    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> getCsrfToken(
            org.springframework.security.web.csrf.CsrfToken token) {
        
        if (token == null) {
            return ResponseEntity.ok(Map.of("message", "CSRF protection disabled"));
        }
        
        return ResponseEntity.ok(Map.of(
            "token", token.getToken(),
            "headerName", token.getHeaderName(),
            "parameterName", token.getParameterName()
        ));
    }

    /**
     * Logout endpoint - invalidates current session.
     * 
     * @return success response
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        sessionService.invalidateSession();
        
        log.info("User logged out successfully");
        
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
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

    /**
     * Exception handler for OTP verification failures.
     */
    @ExceptionHandler(OtpService.OtpVerificationException.class)
    public ResponseEntity<Map<String, String>> handleVerificationException(
            OtpService.OtpVerificationException ex) {
        
        log.warn("OTP verification failed: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("error", ex.getMessage()));
    }
}
