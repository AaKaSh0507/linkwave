package com.linkwave.app.config.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RequestTimingFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(RequestTimingFilter.class);

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    long start = System.currentTimeMillis();

    try {
      filterChain.doFilter(request, response);
    } finally {
      long duration = System.currentTimeMillis() - start;
      String method = request.getMethod();
      String uri = request.getRequestURI();
      int status = response.getStatus();

      if (duration > 1000) {
        log.warn(
            "Slow request: method={} uri={} status={} duration_ms={}",
            method,
            uri,
            status,
            duration);
      } else {
        log.info(
            "Request completed: method={} uri={} status={} duration_ms={}",
            method,
            uri,
            status,
            duration);
      }
    }
  }

  @Override
  protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.startsWith("/actuator") || path.startsWith("/ws");
  }
}
