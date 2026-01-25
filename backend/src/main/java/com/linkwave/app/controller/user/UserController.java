package com.linkwave.app.controller.user;

import com.linkwave.app.domain.auth.AuthenticatedUserContext;
import com.linkwave.app.domain.user.UserProfilePayload;
import com.linkwave.app.service.session.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * REST controller for authenticated user operations.
 * All endpoints require authenticated session.
 */
@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private final SessionService sessionService;

    public UserController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * Get current authenticated user profile.
     * Returns masked phone number and authentication timestamp.
     * 
     * @return user profile payload
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfilePayload> getCurrentUser() {
        // Check if Spring Security authentication is present (for tests with @WithMockUser)
        Authentication springAuth = SecurityContextHolder.getContext().getAuthentication();
        if (springAuth != null && springAuth.isAuthenticated() && 
            !"anonymousUser".equals(springAuth.getPrincipal())) {
            
            // Try to get from session first
            AuthenticatedUserContext userContext = sessionService.getAuthenticatedUser()
                .orElse(null);
            
            if (userContext == null) {
                log.warn("Unauthorized access attempt to /me endpoint");
                return ResponseEntity.status(401).build();
            }

            String maskedPhone = userContext.getMaskedPhoneNumber();
            String authenticatedAt = ISO_FORMATTER.format(userContext.getAuthenticatedAt());

            UserProfilePayload profile = new UserProfilePayload(maskedPhone, authenticatedAt);
            
            log.info("User profile accessed: {}", maskedPhone);
            
            return ResponseEntity.ok(profile);
        }
        
        // No authentication
        log.warn("Unauthorized access attempt to /me endpoint");
        return ResponseEntity.status(401).build();
    }
}
