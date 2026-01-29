package com.linkwave.app.security;

import com.linkwave.app.service.session.SessionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for the application.
 * Enables session-based authentication with Redis storage.
 * Protects user endpoints with authentication requirement.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SessionService sessionService;

    public SecurityConfig(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Configure CSRF token repository to use cookies
        CookieCsrfTokenRepository tokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        tokenRepository.setCookieName("XSRF-TOKEN");
        tokenRepository.setHeaderName("X-XSRF-TOKEN");
        
        // Use default CSRF token request handler
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        
        http
            // Enable CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Add custom session authentication filter
            .addFilterBefore(new SessionAuthenticationFilter(sessionService), 
                           UsernamePasswordAuthenticationFilter.class)
            
            // CSRF Configuration
            .csrf(csrf -> csrf
                // Use cookie-based CSRF tokens (accessible to JavaScript for SPAs)
                .csrfTokenRepository(tokenRepository)
                .csrfTokenRequestHandler(requestHandler)
                // Exempt OTP endpoints - these are email-based, no browser form submission
                .ignoringRequestMatchers("/api/v1/auth/request-otp", "/api/v1/auth/verify-otp")
                // All other POST/PUT/DELETE require CSRF token (including logout)
            )
            
            // Session Management Configuration
            .sessionManagement(session -> session
                // Enable session creation (not stateless)
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                // Maximum one session per user
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            )
            
            // Exception Handling Configuration
            .exceptionHandling(exception -> exception
                // Return 401 for unauthenticated requests (no redirect)
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Unauthorized - Authentication required\"}");
                })
            )
            
            // Authorization Rules
            .authorizeHttpRequests(auth -> auth
                // Public authentication endpoints
                .requestMatchers("/api/v1/auth/**").permitAll()
                // Public health check
                .requestMatchers("/actuator/health").permitAll()
                // WebSocket endpoint - authentication handled by WsAuthenticationInterceptor
                .requestMatchers("/ws/**").permitAll()
                // Protected user endpoints - require authenticated session
                .requestMatchers("/api/v1/user/**").authenticated()
                // Future protected chat endpoints
                // .requestMatchers("/api/v1/chat/**").authenticated()
                // All other requests require authentication
                .anyRequest().authenticated()
            );
        
        return http.build();
    }

    /**
     * CORS configuration for frontend access.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000")); // Next.js dev server
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true); // Allow cookies/session
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
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
