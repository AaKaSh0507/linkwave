package com.linkwave.app.config.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class UserContextFilter extends OncePerRequestFilter {

  private static final String USER_ID_MDC_KEY = "user_id";

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    try {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof String phone) {
        MDC.put(USER_ID_MDC_KEY, maskPhone(phone));
      }
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(USER_ID_MDC_KEY);
    }
  }

  private String maskPhone(String phone) {
    if (phone == null || phone.length() < 4) {
      return "***";
    }
    return "***" + phone.substring(phone.length() - 4);
  }
}
