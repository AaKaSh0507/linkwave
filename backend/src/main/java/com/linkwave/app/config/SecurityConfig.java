package com.linkwave.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;

/**
 * Security configuration for the application.
 * Enables session-based authentication with Redis storage.
 * All endpoints remain public until authentication is implemented.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF Configuration
            .csrf(csrf -> csrf
                // Disable CSRF for OTP endpoints (email-only flow, no browser form submission)
                .ignoringRequestMatchers("/api/v1/auth/request-otp", "/api/v1/auth/verify-otp")
                // Enable CSRF for all other POST endpoints
            )
            
            // Session Management Configuration
            .sessionManagement(session -> session
                // Enable session creation (not stateless)
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                // Maximum one session per user (when authentication is added)
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            )
            
            // Authorization Rules
            .authorizeHttpRequests(auth -> auth
                // Public authentication endpoints
                .requestMatchers("/api/v1/auth/**").permitAll()
                // Public health check
                .requestMatchers("/actuator/health").permitAll()
                // Protected user endpoints (future) - require authenticated session
                // .requestMatchers("/api/v1/user/**").authenticated()
                // .requestMatchers("/api/v1/chat/**").authenticated()
                // All other requests require authentication
                .anyRequest().authenticated()
            );
        
        return http.build();
    }

    /**
     * Bean for session lifecycle event publishing.
     * Required for proper session management.
     */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
