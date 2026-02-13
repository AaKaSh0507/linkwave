package com.linkwave.app;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkwave.app.domain.auth.OtpMetadata;
import com.linkwave.app.domain.chat.ChatMessageEntity;
import com.linkwave.app.domain.chat.ReadReceiptEntity;
import com.linkwave.app.repository.ChatMessageRepository;
import com.linkwave.app.repository.ReadReceiptRepository;
import com.linkwave.app.service.auth.OtpService;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.lang.reflect.Field;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

@Tag("integration")
@Tag("slow")
class WebSocketFunctionalTest extends FunctionalTestBase {

  private static final String AUTH_BASE = "/api/v1/auth";
  private static final String CHAT_BASE = "/api/v1/chat";
  private static final String SESSION_COOKIE = "LINKWAVE_SESSION";

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired private OtpService otpService;

  @Autowired private ChatMessageRepository chatMessageRepository;

  @Autowired private ReadReceiptRepository readReceiptRepository;

  @Test
  void testWebSocketConnection_Success() throws Exception {
    String user = "+14155570101";
    String sessionId = authenticateUser(user);

    TestWebSocketClient client = connectClient(sessionId);
    try {
      assertTrue(client.isOpen(), "WebSocket should be open");
      assertTrue(client.awaitOpen(5, TimeUnit.SECONDS), "onOpen should be called");
      assertTrue(client.getErrors().isEmpty(), "Connection should not produce errors");
    } finally {
      closeQuietly(client);
    }
  }

  @Test
  void testWebSocketConnection_Unauthenticated() throws Exception {
    TestWebSocketClient client = connectClient(null);
    try {
      assertFalse(client.isOpen(), "Unauthenticated websocket should not remain open");
      await()
          .atMost(3, TimeUnit.SECONDS)
          .until(() -> !client.getErrors().isEmpty() || client.awaitClosed(1, TimeUnit.SECONDS));
    } finally {
      closeQuietly(client);
    }
  }

  @Test
  void testWebSocketMessageSending() throws Exception {
    String user1 = "+14155570102";
    String user2 = "+14155570103";

    String user1Session = authenticateUser(user1);
    String user2Session = authenticateUser(user2);
    String roomId = createDirectRoom(user1Session, user2);

    TestWebSocketClient client1 = connectClient(user1Session);
    TestWebSocketClient client2 = connectClient(user2Session);

    try {
      client1.send(
          "{\"event\":\"chat.send\",\"to\":\"" + roomId + "\",\"payload\":{\"body\":\"Hello\"}}");

      await()
          .atMost(5, TimeUnit.SECONDS)
          .until(() -> client1.containsMessagePart("\"event\":\"chat.sent\""));

      await().atMost(5, TimeUnit.SECONDS).until(() -> !latestMessages(roomId, 1).isEmpty());

      ChatMessageEntity persisted = latestMessages(roomId, 1).getFirst();
      assertEquals(user1, persisted.getSenderPhone());
      assertEquals("Hello", persisted.getBody());
      assertNotNull(persisted.getSentAt());

      assertTrue(client1.containsMessagePart("chat.sent"));
      assertTrue(client2.getErrors().isEmpty());
    } finally {
      closeQuietly(client1);
      closeQuietly(client2);
    }
  }

  @Test
  void testWebSocketBidirectionalMessaging() throws Exception {
    String user1 = "+14155570104";
    String user2 = "+14155570105";

    String user1Session = authenticateUser(user1);
    String user2Session = authenticateUser(user2);
    String roomId = createDirectRoom(user1Session, user2);

    TestWebSocketClient client1 = connectClient(user1Session);
    TestWebSocketClient client2 = connectClient(user2Session);

    try {
      client1.send(
          "{\"event\":\"chat.send\",\"to\":\""
              + roomId
              + "\",\"payload\":{\"body\":\"Ping from user1\"}}");
      client2.send(
          "{\"event\":\"chat.send\",\"to\":\""
              + roomId
              + "\",\"payload\":{\"body\":\"Pong from user2\"}}");

      await()
          .atMost(5, TimeUnit.SECONDS)
          .until(
              () ->
                  client1.containsMessagePart("chat.sent")
                      && client2.containsMessagePart("chat.sent"));

      await().atMost(5, TimeUnit.SECONDS).until(() -> latestMessages(roomId, 10).size() >= 2);

      List<ChatMessageEntity> persisted = latestMessages(roomId, 10);
      assertTrue(
          persisted.stream()
              .anyMatch(
                  m -> user1.equals(m.getSenderPhone()) && "Ping from user1".equals(m.getBody())));
      assertTrue(
          persisted.stream()
              .anyMatch(
                  m -> user2.equals(m.getSenderPhone()) && "Pong from user2".equals(m.getBody())));
    } finally {
      closeQuietly(client1);
      closeQuietly(client2);
    }
  }

  @Test
  void testWebSocketReconnection() throws Exception {
    String user1 = "+14155570106";
    String user2 = "+14155570107";

    String user1Session = authenticateUser(user1);
    String user2Session = authenticateUser(user2);
    String roomId = createDirectRoom(user1Session, user2);

    TestWebSocketClient client = connectClient(user1Session);
    try {
      assertTrue(client.isOpen());
      closeQuietly(client);
      Thread.sleep(2000);

      TestWebSocketClient reconnectedClient = connectClient(user1Session);
      client = reconnectedClient;
      assertTrue(reconnectedClient.isOpen(), "Reconnection should succeed with same session");

      reconnectedClient.send(
          "{\"event\":\"chat.send\",\"to\":\""
              + roomId
              + "\",\"payload\":{\"body\":\"Message after reconnect\"}}");

      await()
          .atMost(5, TimeUnit.SECONDS)
          .until(() -> reconnectedClient.containsMessagePart("chat.sent"));
    } finally {
      closeQuietly(client);
    }
  }

  @Test
  void testPresenceUpdateViaWebSocket() throws Exception {
    String user1 = "+14155570108";
    String session = authenticateUser(user1);

    TestWebSocketClient client = connectClient(session);
    String presenceKey = "linkwave:presence:" + user1;

    try {
      if (stringRedisTemplate == null) {
        return;
      }

      client.send("{\"event\":\"presence.heartbeat\"}");

      await()
          .atMost(5, TimeUnit.SECONDS)
          .until(() -> client.containsMessagePart("presence.heartbeat.ack"));

      await()
          .atMost(5, TimeUnit.SECONDS)
          .until(() -> Boolean.TRUE.equals(stringRedisTemplate.hasKey(presenceKey)));

      closeQuietly(client);

      stringRedisTemplate.expire(presenceKey, Duration.ofSeconds(1));

      await()
          .atMost(5, TimeUnit.SECONDS)
          .until(() -> !Boolean.TRUE.equals(stringRedisTemplate.hasKey(presenceKey)));
    } finally {
      closeQuietly(client);
    }
  }

  @Test
  void testTypingIndicator() throws Exception {
    String user1 = "+14155570109";
    String user2 = "+14155570110";

    String user1Session = authenticateUser(user1);
    String user2Session = authenticateUser(user2);
    String roomId = createDirectRoom(user1Session, user2);

    TestWebSocketClient client1 = connectClient(user1Session);
    TestWebSocketClient client2 = connectClient(user2Session);

    try {
      client1.send("{\"event\":\"typing.start\",\"roomId\":\"" + roomId + "\"}");

      await()
          .atMost(5, TimeUnit.SECONDS)
          .until(
              () ->
                  client2.containsMessagePart("\"type\":\"typing.event\"")
                      && client2.containsMessagePart("\"action\":\"start\""));

      await()
          .atMost(12, TimeUnit.SECONDS)
          .until(() -> client2.containsMessagePart("\"action\":\"stop\""));
    } finally {
      closeQuietly(client1);
      closeQuietly(client2);
    }
  }

  @Test
  void testReadReceipt() throws Exception {
    String user1 = "+14155570111";
    String user2 = "+14155570112";

    String user1Session = authenticateUser(user1);
    String user2Session = authenticateUser(user2);
    String roomId = createDirectRoom(user1Session, user2);

    TestWebSocketClient client1 = connectClient(user1Session);
    TestWebSocketClient client2 = connectClient(user2Session);

    try {
      client1.send(
          "{\"event\":\"chat.send\",\"to\":\""
              + roomId
              + "\",\"payload\":{\"body\":\"read-receipt-message\"}}");

      await().atMost(5, TimeUnit.SECONDS).until(() -> client1.containsMessagePart("chat.sent"));

      await().atMost(5, TimeUnit.SECONDS).until(() -> !latestMessages(roomId, 1).isEmpty());
      String messageId = latestMessages(roomId, 1).getFirst().getId();

      client2.send(
          "{\"event\":\"read.up_to\",\"roomId\":\""
              + roomId
              + "\",\"messageId\":\""
              + messageId
              + "\"}");

      await()
          .atMost(5, TimeUnit.SECONDS)
          .until(
              () -> readReceiptRepository.existsByMessageIdAndReaderPhoneNumber(messageId, user2));

      List<ReadReceiptEntity> receipts = readReceiptRepository.findByMessageId(messageId);
      assertTrue(receipts.stream().anyMatch(r -> user2.equals(r.getReaderPhoneNumber())));

      await()
          .atMost(5, TimeUnit.SECONDS)
          .until(
              () ->
                  client1.containsMessagePart("\"type\":\"read.receipt\"")
                      && client1.containsMessagePart(messageId));
    } finally {
      closeQuietly(client1);
      closeQuietly(client2);
    }
  }

  @Test
  void testWebSocketPingPong() throws Exception {
    String user = "+14155570113";
    String sessionId = authenticateUser(user);

    TestWebSocketClient client = connectClient(sessionId);
    try {
      assertTrue(client.isOpen());

      client.send("{\"event\":\"ping\"}");

      await()
          .atMost(5, TimeUnit.SECONDS)
          .until(() -> client.containsMessagePart("\"event\":\"pong\""));

      assertTrue(client.containsMessagePart("timestamp"));
    } finally {
      closeQuietly(client);
    }
  }

  @Test
  void testWebSocketInvalidMessageFormat() throws Exception {
    String user = "+14155570114";
    String sessionId = authenticateUser(user);

    TestWebSocketClient client = connectClient(sessionId);
    try {
      assertTrue(client.isOpen());

      client.send("this is not valid json");

      await()
          .atMost(5, TimeUnit.SECONDS)
          .until(() -> !client.isOpen() || !client.getErrors().isEmpty());
    } finally {
      closeQuietly(client);
    }
  }

  @Test
  void testWebSocketConnectionAck() throws Exception {
    String user = "+14155570115";
    String sessionId = authenticateUser(user);

    TestWebSocketClient client = connectClient(sessionId);
    try {
      assertTrue(client.isOpen());

      await()
          .atMost(5, TimeUnit.SECONDS)
          .until(() -> client.containsMessagePart("\"event\":\"connection.ack\""));

      assertTrue(client.containsMessagePart("connected"));
    } finally {
      closeQuietly(client);
    }
  }

  private List<ChatMessageEntity> latestMessages(String roomId, int size) {
    return chatMessageRepository
        .findByRoomOrderBySentAtDesc(
            jdbcTemplate.queryForObject(
                "SELECT id, room_type, name, created_at, updated_at FROM chat_rooms WHERE id = ?",
                (rs, rowNum) -> {
                  com.linkwave.app.domain.chat.ChatRoomEntity room =
                      new com.linkwave.app.domain.chat.ChatRoomEntity();
                  room.setId(rs.getString("id"));
                  room.setRoomType(
                      com.linkwave.app.domain.chat.ChatRoomEntity.RoomType.valueOf(
                          rs.getString("room_type")));
                  room.setName(rs.getString("name"));
                  room.setCreatedAt(rs.getTimestamp("created_at").toInstant());
                  room.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
                  return room;
                },
                roomId),
            PageRequest.of(0, size))
        .getContent();
  }

  private String authenticateUser(String phone) {
    String email = "ws-auth-" + phone.substring(phone.length() - 4) + "@example.com";

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("phoneNumber", phone, "email", email))
        .when()
        .post(AUTH_BASE + "/request-otp")
        .then()
        .statusCode(200);

    OtpMetadata metadata = getOtpStore().get(phone);
    assertNotNull(metadata, "OTP should exist for authentication flow");

    Response response =
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("phoneNumber", phone, "otp", metadata.getOtpValue()))
            .when()
            .post(AUTH_BASE + "/verify-otp")
            .then()
            .statusCode(200)
            .extract()
            .response();

    String sessionId = response.getCookie(SESSION_COOKIE);
    assertNotNull(sessionId, "Session cookie should be available after authentication");
    return sessionId;
  }

  private String createDirectRoom(String creatorSessionId, String otherUserPhone) {
    Response response =
        given()
            .cookie(SESSION_COOKIE, creatorSessionId)
            .contentType(ContentType.JSON)
            .body(Map.of("otherUserPhone", otherUserPhone))
            .when()
            .post(CHAT_BASE + "/rooms/direct")
            .then()
            .statusCode(200)
            .extract()
            .response();

    String roomId = response.jsonPath().getString("id");
    assertNotNull(roomId);
    return roomId;
  }

  private TestWebSocketClient connectClient(String sessionId) throws Exception {
    Map<String, String> headers = new java.util.HashMap<>();
    headers.put("Origin", "http://localhost:3000");
    if (sessionId != null) {
      headers.put("Cookie", SESSION_COOKIE + "=" + sessionId);
    }

    TestWebSocketClient client =
        new TestWebSocketClient(new URI("ws://localhost:" + port + "/ws"), headers);

    client.connectBlocking(5, TimeUnit.SECONDS);
    return client;
  }

  private void closeQuietly(TestWebSocketClient client) {
    if (client == null) {
      return;
    }
    try {
      if (client.isOpen()) {
        client.closeBlocking();
      }
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, OtpMetadata> getOtpStore() {
    try {
      Field field = OtpService.class.getDeclaredField("otpStore");
      field.setAccessible(true);
      return (Map<String, OtpMetadata>) field.get(otpService);
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException("Unable to access otpStore for functional tests", ex);
    }
  }

  private static class TestWebSocketClient extends WebSocketClient {

    private final CountDownLatch openLatch = new CountDownLatch(1);
    private final CountDownLatch closeLatch = new CountDownLatch(1);
    private final List<String> messages = new CopyOnWriteArrayList<>();
    private final List<Throwable> errors = new CopyOnWriteArrayList<>();

    TestWebSocketClient(URI serverUri, Map<String, String> httpHeaders) {
      super(serverUri, httpHeaders);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
      openLatch.countDown();
    }

    @Override
    public void onMessage(String message) {
      messages.add(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
      closeLatch.countDown();
    }

    @Override
    public void onError(Exception ex) {
      errors.add(ex);
    }

    boolean awaitOpen(long timeout, TimeUnit unit) throws InterruptedException {
      return openLatch.await(timeout, unit);
    }

    boolean awaitClosed(long timeout, TimeUnit unit) throws InterruptedException {
      return closeLatch.await(timeout, unit);
    }

    boolean containsMessagePart(String text) {
      for (String message : messages) {
        if (message != null && message.contains(text)) {
          return true;
        }
        try {
          JsonNode node = new ObjectMapper().readTree(message);
          if (node.toString().contains(text)) {
            return true;
          }
        } catch (Exception ignored) {
          // best-effort parse only
        }
      }
      return false;
    }

    List<String> getMessages() {
      return new ArrayList<>(messages);
    }

    List<Throwable> getErrors() {
      return new ArrayList<>(errors);
    }
  }
}
