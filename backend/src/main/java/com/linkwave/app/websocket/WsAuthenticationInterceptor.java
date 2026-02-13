package com.linkwave.app.websocket;

import com.linkwave.app.domain.auth.AuthenticatedUserContext;
import com.linkwave.app.service.session.SessionService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
public class WsAuthenticationInterceptor implements HandshakeInterceptor {

  private static final Logger log = LoggerFactory.getLogger(WsAuthenticationInterceptor.class);

  private final SessionService sessionService;

  public WsAuthenticationInterceptor(SessionService sessionService) {
    this.sessionService = sessionService;
  }

  @Override
  public boolean beforeHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Map<String, Object> attributes)
      throws Exception {

    var authenticatedUser = sessionService.getAuthenticatedUser();

    if (authenticatedUser.isEmpty()) {
      log.warn(
          "WebSocket handshake rejected: Not authenticated (remote: {})",
          request.getRemoteAddress());
      response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
      return false;
    }

    AuthenticatedUserContext userContext = authenticatedUser.get();
    String phoneNumber = userContext.getPhoneNumber();
    attributes.put("phoneNumber", phoneNumber);
    attributes.put("authenticatedAt", userContext.getAuthenticatedAt());

    log.info(
        "WebSocket handshake accepted for user: {} (remote: {})",
        userContext.getMaskedPhoneNumber(),
        request.getRemoteAddress());

    return true;
  }

  @Override
  public void afterHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Exception exception) {
    if (exception != null) {
      log.error("WebSocket handshake error: {}", exception.getMessage());
    }
  }
}
