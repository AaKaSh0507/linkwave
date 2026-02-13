package com.linkwave.app.security;

import com.linkwave.app.config.logging.CorrelationIdFilter;
import com.linkwave.app.config.logging.UserContextFilter;
import com.linkwave.app.service.session.SessionService;
import java.util.Arrays;
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

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final SessionService sessionService;
  private final CorrelationIdFilter correlationIdFilter;
  private final UserContextFilter userContextFilter;

  public SecurityConfig(
      SessionService sessionService,
      CorrelationIdFilter correlationIdFilter,
      UserContextFilter userContextFilter) {
    this.sessionService = sessionService;
    this.correlationIdFilter = correlationIdFilter;
    this.userContextFilter = userContextFilter;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    CookieCsrfTokenRepository tokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
    tokenRepository.setCookieName("XSRF-TOKEN");
    tokenRepository.setHeaderName("X-XSRF-TOKEN");
    CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();

    http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(new SessionAuthenticationFilter(sessionService), CorrelationIdFilter.class)
        .addFilterAfter(userContextFilter, SessionAuthenticationFilter.class)
        .csrf(
            csrf ->
                csrf.csrfTokenRepository(tokenRepository)
                    .csrfTokenRequestHandler(requestHandler)
                    .ignoringRequestMatchers("/api/v1/auth/request-otp", "/api/v1/auth/verify-otp"))
        .sessionManagement(
            session ->
                session
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                    .maximumSessions(1)
                    .maxSessionsPreventsLogin(false))
        .exceptionHandling(
            exception ->
                exception.authenticationEntryPoint(
                    (request, response, authException) -> {
                      response.setStatus(401);
                      response.setContentType("application/json");
                      response
                          .getWriter()
                          .write("{\"error\":\"Unauthorized - Authentication required\"}");
                    }))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/v1/auth/**")
                    .permitAll()
                    .requestMatchers("/error")
                    .permitAll()
                    .requestMatchers("/actuator/health", "/actuator/prometheus")
                    .permitAll()
                    .requestMatchers("/docs", "/docs/**", "/swagger-ui/**", "/v3/api-docs/**")
                    .permitAll()
                    .requestMatchers("/ws/**")
                    .permitAll()
                    .requestMatchers("/api/v1/user/**")
                    .authenticated()
                    .requestMatchers("/api/v1/chat/**")
                    .authenticated()
                    .anyRequest()
                    .authenticated());

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    String allowedOrigins =
        System.getenv().getOrDefault("CORS_ALLOWED_ORIGINS", "http://localhost:3000");
    configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public HttpSessionEventPublisher httpSessionEventPublisher() {
    return new HttpSessionEventPublisher();
  }
}
