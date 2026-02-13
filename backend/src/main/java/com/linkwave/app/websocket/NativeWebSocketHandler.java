package com.linkwave.app.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkwave.app.domain.chat.ReadReceiptEntity;
import com.linkwave.app.domain.chat.ReadReceiptEvent;
import com.linkwave.app.domain.typing.TypingEvent;
import com.linkwave.app.domain.websocket.WsMessageEnvelope;
import com.linkwave.app.exception.NotFoundException;
import com.linkwave.app.exception.UnauthorizedException;
import com.linkwave.app.service.chat.ChatService;
import com.linkwave.app.service.presence.PresenceService;
import com.linkwave.app.service.readreceipt.ReadReceiptService;
import com.linkwave.app.service.readreceipt.ReadReceiptService.ReadReceiptResult;
import com.linkwave.app.service.room.RoomMembershipService;
import com.linkwave.app.service.typing.TypingStateManager;
import com.linkwave.app.service.websocket.WsSessionManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class NativeWebSocketHandler extends TextWebSocketHandler {

  private static final Logger log = LoggerFactory.getLogger(NativeWebSocketHandler.class);

  private final PresenceService presenceService;
  private final TypingStateManager typingStateManager;
  private final RoomMembershipService roomMembershipService;
  private final ReadReceiptService readReceiptService;
  private final ChatService chatService;
  private final WsSessionManager sessionManager;
  private final ObjectMapper objectMapper;
  private final Counter wsConnectedCounter;
  private final Counter wsDisconnectedCounter;
  private final Counter wsInboundCounter;
  private final Counter wsOutboundCounter;
  private final Counter wsErrorConnection;
  private final Counter wsErrorMessage;
  private final Counter wsErrorAuth;
  private final Timer wsMessageProcessingTimer;

  public NativeWebSocketHandler(
      PresenceService presenceService,
      TypingStateManager typingStateManager,
      RoomMembershipService roomMembershipService,
      ReadReceiptService readReceiptService,
      ChatService chatService,
      WsSessionManager sessionManager,
      ObjectMapper objectMapper,
      MeterRegistry meterRegistry) {
    this.presenceService = presenceService;
    this.typingStateManager = typingStateManager;
    this.roomMembershipService = roomMembershipService;
    this.readReceiptService = readReceiptService;
    this.chatService = chatService;
    this.sessionManager = sessionManager;
    this.objectMapper = objectMapper;

    Gauge.builder(
            "websocket.connections.active", sessionManager, WsSessionManager::getActiveSessionCount)
        .register(meterRegistry);
    this.wsConnectedCounter =
        Counter.builder("websocket.connections.total")
            .tag("event", "connected")
            .register(meterRegistry);
    this.wsDisconnectedCounter =
        Counter.builder("websocket.connections.total")
            .tag("event", "disconnected")
            .register(meterRegistry);
    this.wsInboundCounter =
        Counter.builder("websocket.messages.total")
            .tag("direction", "inbound")
            .register(meterRegistry);
    this.wsOutboundCounter =
        Counter.builder("websocket.messages.total")
            .tag("direction", "outbound")
            .register(meterRegistry);
    this.wsErrorConnection =
        Counter.builder("websocket.errors.total").tag("type", "connection").register(meterRegistry);
    this.wsErrorMessage =
        Counter.builder("websocket.errors.total").tag("type", "message").register(meterRegistry);
    this.wsErrorAuth =
        Counter.builder("websocket.errors.total")
            .tag("type", "authentication")
            .register(meterRegistry);
    this.wsMessageProcessingTimer =
        Timer.builder("websocket.message.processing.duration").register(meterRegistry);
  }

  @Override
  public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
    String phoneNumber = (String) session.getAttributes().get("phoneNumber");

    if (phoneNumber == null) {
      log.warn("WebSocket connection without phoneNumber attribute, closing");
      wsErrorAuth.increment();
      @SuppressWarnings("nullness")
      CloseStatus policyViolation = CloseStatus.POLICY_VIOLATION;
      session.close(policyViolation);
      return;
    }

    sessionManager.registerSession(phoneNumber, session);
    presenceService.markOnline(phoneNumber);
    wsConnectedCounter.increment();

    log.info(
        "WebSocket connection established for user: {} (sessionId: {})",
        maskPhoneNumber(phoneNumber),
        session.getId());
    sendMessage(session, "{\"event\":\"connection.ack\",\"status\":\"connected\"}");
  }

  @Override
  protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message)
      throws Exception {
    String phoneNumber = (String) session.getAttributes().get("phoneNumber");
    String payload = message.getPayload();
    wsInboundCounter.increment();

    log.debug("Received message from {}: {}", maskPhoneNumber(phoneNumber), payload);
    Timer.Sample sample = Timer.start();
    try {
      JsonNode jsonNode = objectMapper.readTree(payload);
      String messageType = jsonNode.has("event") ? jsonNode.get("event").asText() : null;

      if (messageType == null) {
        log.warn("Message from {} missing 'event' field", maskPhoneNumber(phoneNumber));
        wsErrorMessage.increment();
        if (session.isOpen()) {
          session.close(CloseStatus.BAD_DATA);
        }
        return;
      }

      switch (messageType) {
        case "ping":
          handlePing(session, phoneNumber);
          break;

        case "chat.send":
          handleChatSend(session, phoneNumber, jsonNode);
          break;

        case "presence.heartbeat":
          handlePresenceHeartbeat(session, phoneNumber);
          break;

        case "typing.start":
          handleTypingStart(session, phoneNumber, jsonNode);
          break;

        case "typing.stop":
          handleTypingStop(session, phoneNumber, jsonNode);
          break;

        case "read.up_to":
          handleReadUpTo(session, phoneNumber, jsonNode);
          break;

        default:
          log.debug("Unhandled message type: {}", messageType);
          sendMessage(session, "{\"event\":\"message.ack\",\"received\":true}");
          break;
      }

    } catch (Exception e) {
      wsErrorMessage.increment();
      log.error(
          "Failed to parse message from {}: {}", maskPhoneNumber(phoneNumber), e.getMessage());
      if (session.isOpen()) {
        session.close(CloseStatus.BAD_DATA);
      }
    } finally {
      sample.stop(wsMessageProcessingTimer);
    }
  }

  private void handlePing(WebSocketSession session, String userId) {
    sendMessage(session, "{\"event\":\"pong\",\"timestamp\":" + System.currentTimeMillis() + "}");
  }

  private void handleChatSend(WebSocketSession session, String userId, JsonNode jsonNode) {
    try {
      WsMessageEnvelope envelope = objectMapper.treeToValue(jsonNode, WsMessageEnvelope.class);
      String body = "";
      if (envelope.getPayload() != null) {
        if (envelope.getPayload().has("body")) {
          body = envelope.getPayload().get("body").asText();
        } else {
          body = envelope.getPayload().toString();
        }
      }

      chatService.sendMessage(envelope.getTo(), userId, body);
      String messageId = java.util.UUID.randomUUID().toString();
      sendMessage(
          session,
          String.format("{\"event\":\"chat.sent\",\"payload\":{\"messageId\":\"%s\"}}", messageId));

    } catch (Exception e) {
      log.error(
          "Failed to process chat.send from user {}: {}", maskPhoneNumber(userId), e.getMessage());
    }
  }

  private void handlePresenceHeartbeat(WebSocketSession session, String phoneNumber) {
    boolean success = presenceService.recordHeartbeat(phoneNumber);

    String status = success ? "ok" : "rate_limited";
    String response =
        String.format("{\"event\":\"presence.heartbeat.ack\",\"status\":\"%s\"}", status);

    sendMessage(session, response);

    if (success) {
      log.debug("Heartbeat recorded for user: {}", maskPhoneNumber(phoneNumber));
    } else {
      log.debug("Heartbeat rate-limited for user: {}", maskPhoneNumber(phoneNumber));
    }
  }

  private void handleTypingStart(WebSocketSession session, String userId, JsonNode payload) {
    String roomId = payload.has("roomId") ? payload.get("roomId").asText() : null;

    if (roomId == null) {
      log.warn("typing.start missing roomId from user {}", maskPhoneNumber(userId));
      return;
    }

    if (!roomMembershipService.isUserInRoom(userId, roomId)) {
      log.warn("User {} not in room {}, ignoring typing.start", maskPhoneNumber(userId), roomId);
      return;
    }
    boolean accepted = typingStateManager.markTypingStart(roomId, userId, session.getId());

    if (!accepted) {
      log.debug(
          "typing.start rate-limited for user {} in room {}", maskPhoneNumber(userId), roomId);
      return;
    }
    broadcastTypingEvent(roomId, userId, TypingEvent.TypingAction.START);
  }

  private void handleTypingStop(WebSocketSession session, String userId, JsonNode payload) {
    String roomId = payload.has("roomId") ? payload.get("roomId").asText() : null;

    if (roomId == null) {
      log.warn("typing.stop missing roomId from user {}", maskPhoneNumber(userId));
      return;
    }
    typingStateManager.markTypingStop(roomId, userId, session.getId());
    broadcastTypingEvent(roomId, userId, TypingEvent.TypingAction.STOP);
  }

  private void broadcastTypingEvent(
      String roomId, String senderId, TypingEvent.TypingAction action) {
    try {
      Set<String> members = roomMembershipService.getRoomMembers(roomId);
      if (members.isEmpty()) {
        return;
      }

      TypingEvent event = new TypingEvent(senderId, roomId, action);
      String json = objectMapper.writeValueAsString(event);

      for (String memberId : members) {
        if (!memberId.equals(senderId)) {
          sendToUser(memberId, json);
        }
      }

      log.debug(
          "Broadcasted typing.{} for user {} in room {} to {} members",
          action.name().toLowerCase(),
          maskPhoneNumber(senderId),
          roomId,
          members.size() - 1);

    } catch (Exception e) {
      log.error("Error broadcasting typing event: {}", e.getMessage());
    }
  }

  private void handleReadUpTo(WebSocketSession session, String userId, JsonNode payload) {
    String roomId = payload.has("roomId") ? payload.get("roomId").asText() : null;
    String messageId = payload.has("messageId") ? payload.get("messageId").asText() : null;

    if (roomId == null || messageId == null) {
      log.warn("read.up_to missing roomId or messageId from user {}", maskPhoneNumber(userId));
      return;
    }

    try {
      List<ReadReceiptResult> results = readReceiptService.markReadUpTo(roomId, messageId, userId);
      boolean hasNewReads = results.stream().anyMatch(ReadReceiptResult::isNewRead);
      if (hasNewReads) {
        Set<String> members = roomMembershipService.getRoomMembers(roomId);
        for (ReadReceiptResult result : results) {
          if (result.isNewRead()) {
            broadcastReadReceipt(result.getReceipt(), members);
          }
        }
      }

      log.debug(
          "User {} marked {} messages as read in room {}",
          maskPhoneNumber(userId),
          results.size(),
          roomId);

    } catch (NotFoundException e) {
      log.warn("Message not found: {}", messageId);
    } catch (UnauthorizedException e) {
      log.warn("User {} not authorized to read in room {}", maskPhoneNumber(userId), roomId);
    } catch (Exception e) {
      log.error("Error processing read receipt: {}", e.getMessage());
    }
  }

  private void broadcastReadReceipt(ReadReceiptEntity receipt, Set<String> members) {
    try {
      if (members.isEmpty()) {
        return;
      }

      ReadReceiptEvent event =
          new ReadReceiptEvent(
              receipt.getRoomId(),
              receipt.getMessageId(),
              receipt.getReaderPhoneNumber(),
              receipt.getReadAt().toEpochMilli());

      String json = objectMapper.writeValueAsString(event);

      for (String memberId : members) {
        if (!memberId.equals(receipt.getReaderPhoneNumber())) {
          sendToUser(memberId, json);
        }
      }

      log.debug(
          "Broadcasted read receipt for message {} in room {} to {} members",
          receipt.getMessageId(),
          receipt.getRoomId(),
          members.size() - 1);

    } catch (Exception e) {
      log.error("Error broadcasting read receipt: {}", e.getMessage());
    }
  }

  @Override
  public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status)
      throws Exception {
    String phoneNumber = (String) session.getAttributes().get("phoneNumber");

    if (phoneNumber != null) {
      sessionManager.deregisterSession(session);
      presenceService.markDisconnect(phoneNumber);
      wsDisconnectedCounter.increment();
      List<String> affectedRooms = typingStateManager.clearUserTyping(phoneNumber, session.getId());
      for (String roomId : affectedRooms) {
        broadcastTypingEvent(roomId, phoneNumber, TypingEvent.TypingAction.STOP);
      }

      log.info(
          "WebSocket connection closed for user: {} (status: {})",
          maskPhoneNumber(phoneNumber),
          status);
    }
  }

  @Override
  public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception)
      throws Exception {
    String phoneNumber = (String) session.getAttributes().get("phoneNumber");
    wsErrorConnection.increment();
    log.error("WebSocket transport error for user: {}", maskPhoneNumber(phoneNumber), exception);

    if (session.isOpen()) {
      @SuppressWarnings("nullness")
      CloseStatus serverError = CloseStatus.SERVER_ERROR;
      session.close(serverError);
    }
  }

  private void sendMessage(WebSocketSession session, String message) {
    try {
      if (session.isOpen()) {
        @SuppressWarnings("nullness")
        TextMessage textMessage = new TextMessage(message);
        session.sendMessage(textMessage);
        wsOutboundCounter.increment();
      }
    } catch (IOException e) {
      log.error("Failed to send message to session {}: {}", session.getId(), e.getMessage());
    }
  }

  public void sendToUser(String phoneNumber, String message) {
    sessionManager.getSession(phoneNumber).ifPresent(session -> sendMessage(session, message));
  }

  public int getActiveConnectionCount() {
    return sessionManager.getActiveSessionCount();
  }

  private String maskPhoneNumber(String phoneNumber) {
    if (phoneNumber == null || phoneNumber.length() < 7) {
      return "***";
    }
    return phoneNumber.substring(0, 4) + "***" + phoneNumber.substring(phoneNumber.length() - 2);
  }
}
