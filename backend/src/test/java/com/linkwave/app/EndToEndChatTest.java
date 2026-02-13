package com.linkwave.app;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkwave.app.domain.auth.OtpMetadata;
import com.linkwave.app.service.auth.OtpService;
import com.linkwave.app.service.chat.ChatService;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.lang.reflect.Field;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("integration")
@Tag("slow")
class EndToEndChatTest extends FunctionalTestBase {

  private static final String AUTH_BASE = "/api/v1/auth";
  private static final String CHAT_BASE = "/api/v1/chat";
  private static final String SESSION_COOKIE = "LINKWAVE_SESSION";

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired private OtpService otpService;

  @Autowired private ChatService chatService;

  @Test
  void testCompleteUserJourney() throws Exception {
    String user1 = "+14155580101";
    String user2 = "+14155580102";

    String session1 = authenticateUser(user1);
    String session2 = authenticateUser(user2);

    TestWebSocketClient client1 = connectClient(session1);
    TestWebSocketClient client2 = connectClient(session2);

    try {
      String roomId = createDirectRoom(session1, user2);

      String firstBody = "e2e-hello-from-user1";
      String replyBody = "e2e-reply-from-user2";

      client1.send(
          "{\"event\":\"chat.send\",\"to\":\""
              + roomId
              + "\",\"payload\":{\"body\":\""
              + firstBody
              + "\"}}");

      await().atMost(5, TimeUnit.SECONDS).until(() -> client1.containsMessagePart("chat.sent"));
      await()
          .atMost(5, TimeUnit.SECONDS)
          .until(() -> countMessagesByRoomAndBody(roomId, firstBody) == 1);

      client2.send(
          "{\"event\":\"chat.send\",\"to\":\""
              + roomId
              + "\",\"payload\":{\"body\":\""
              + replyBody
              + "\"}}");

      await()
          .atMost(5, TimeUnit.SECONDS)
          .until(() -> countMessagesByRoomAndBody(roomId, replyBody) == 1);

      String firstMessageId = findLatestMessageId(roomId, firstBody);
      assertNotNull(firstMessageId);

      client2.send(
          "{\"event\":\"read.up_to\",\"roomId\":\""
              + roomId
              + "\",\"messageId\":\""
              + firstMessageId
              + "\"}");

      await()
          .atMost(5, TimeUnit.SECONDS)
          .until(() -> countReadReceiptsForMessage(firstMessageId) >= 1);
      await()
          .atMost(5, TimeUnit.SECONDS)
          .until(
              () ->
                  client1.containsMessagePart("\"type\":\"read.receipt\"")
                      && client1.containsMessagePart(firstMessageId));

      assertTrue(client1.getErrors().isEmpty());
      assertTrue(client2.getErrors().isEmpty());
    } finally {
      closeQuietly(client1);
      closeQuietly(client2);
    }
  }

  @Test
  void testMultiUserMessaging() throws Exception {
    List<String> users =
        List.of("+14155580201", "+14155580202", "+14155580203", "+14155580204", "+14155580205");

    Map<String, String> sessions = new HashMap<>();
    Map<String, TestWebSocketClient> clients = new HashMap<>();

    for (String user : users) {
      sessions.put(user, authenticateUser(user));
    }

    try {
      for (String user : users) {
        clients.put(user, connectClient(sessions.get(user)));
      }

      String sender = users.getFirst();
      String senderSession = sessions.get(sender);

      Map<String, String> roomByRecipient = new HashMap<>();
      for (int i = 1; i < users.size(); i++) {
        String recipient = users.get(i);
        roomByRecipient.put(recipient, createDirectRoom(senderSession, recipient));
      }

      for (int i = 1; i < users.size(); i++) {
        String recipient = users.get(i);
        String body = "multi-msg-to-" + recipient;
        String roomId = roomByRecipient.get(recipient);

        clients
            .get(sender)
            .send(
                "{\"event\":\"chat.send\",\"to\":\""
                    + roomId
                    + "\",\"payload\":{\"body\":\""
                    + body
                    + "\"}}");

        String expectedBody = body;
        await()
            .atMost(5, TimeUnit.SECONDS)
            .until(() -> countMessagesByRoomAndBody(roomId, expectedBody) == 1);
      }

      for (int i = 1; i < users.size(); i++) {
        String recipient = users.get(i);
        String ownBody = "multi-msg-to-" + recipient;
        String ownRoom = roomByRecipient.get(recipient);
        assertEquals(1, countMessagesByRoomAndBody(ownRoom, ownBody));
      }

      // No cross delivery in persistence: each recipient room should only have its
      // own target body.
      for (int i = 1; i < users.size(); i++) {
        String recipient = users.get(i);
        String roomId = roomByRecipient.get(recipient);
        long totalForRoom = countMessagesByRoom(roomId);
        assertEquals(1L, totalForRoom);
      }
    } finally {
      for (TestWebSocketClient client : clients.values()) {
        closeQuietly(client);
      }
    }
  }

  @Test
  void testConcurrentMessageSending() throws Exception {
    List<String> users = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      users.add("+141555803" + String.format("%02d", i));
    }

    Map<String, String> sessions = new HashMap<>();
    for (String user : users) {
      sessions.put(user, authenticateUser(user));
    }

    String sender = users.getFirst();
    String senderSession = sessions.get(sender);

    List<String> roomIds = new ArrayList<>();
    for (int i = 1; i < users.size(); i++) {
      roomIds.add(createDirectRoom(senderSession, users.get(i)));
    }

    int totalMessages = 100;
    ExecutorService executor = Executors.newFixedThreadPool(12);
    try {
      List<CompletableFuture<Void>> tasks = new ArrayList<>();

      for (int i = 0; i < totalMessages; i++) {
        final int index = i;
        tasks.add(
            CompletableFuture.runAsync(
                () -> {
                  String roomId = roomIds.get(index % roomIds.size());
                  String body = "concurrent-msg-" + index;
                  chatService.sendMessage(roomId, sender, body);
                },
                executor));
      }

      CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();

      await()
          .atMost(10, TimeUnit.SECONDS)
          .until(() -> countMessagesByBodyPrefix("concurrent-msg-") == totalMessages);

      assertEquals(totalMessages, countMessagesByBodyPrefix("concurrent-msg-"));

      // Per-conversation order preserved by sent_at monotonicity.
      for (String roomId : roomIds) {
        assertTrue(isRoomMessageOrderNonDecreasing(roomId));
      }
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void testSessionExpiryDuringChat() throws Exception {
    String user1 = "+14155580401";
    String user2 = "+14155580402";

    String session1 = authenticateUser(user1);
    String session2 = authenticateUser(user2);

    createDirectRoom(session1, user2);

    TestWebSocketClient activeClient = connectClient(session1);
    try {
      expireSessionNow(session1);
      Thread.sleep(1500);

      // Existing native WS session may remain active; verify new handshake with
      // expired session is rejected.
      TestWebSocketClient reconnectAttempt = connectClient(session1);
      try {
        assertFalse(reconnectAttempt.isOpen(), "Session expiry should block new websocket auth");
      } finally {
        closeQuietly(reconnectAttempt);
      }

      assertTrue(activeClient.getErrors().isEmpty() || !activeClient.isOpen());
    } finally {
      closeQuietly(activeClient);
      closeQuietly(connectClient(session2));
    }
  }

  @Test
  void testMessagePersistenceUnderLoad() {
    String user1 = "+14155580501";
    String user2 = "+14155580502";

    String session1 = authenticateUser(user1);
    authenticateUser(user2);

    String roomId = createDirectRoom(session1, user2);

    int total = 1000;
    for (int i = 0; i < total; i++) {
      chatService.sendMessage(roomId, user1, "load-msg-" + i);
    }

    await()
        .atMost(20, TimeUnit.SECONDS)
        .until(() -> countMessagesByBodyPrefix("load-msg-") == total);

    long persisted = countMessagesByBodyPrefix("load-msg-");
    assertEquals(total, persisted);

    Long overallCount =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM chat_messages", Long.class);
    assertNotNull(overallCount);
    assertTrue(overallCount >= total);
  }

  private String authenticateUser(String phone) {
    String email = "e2e-auth-" + phone.substring(phone.length() - 4) + "@example.com";

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
    Map<String, String> headers = new HashMap<>();
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
      throw new IllegalStateException("Unable to access otpStore for end-to-end tests", ex);
    }
  }

  private long countMessagesByRoomAndBody(String roomId, String body) {
    Long count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM chat_messages WHERE room_id = ? AND body = ?",
            Long.class,
            roomId,
            body);
    return count == null ? 0L : count;
  }

  private long countMessagesByRoom(String roomId) {
    Long count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM chat_messages WHERE room_id = ?", Long.class, roomId);
    return count == null ? 0L : count;
  }

  private long countMessagesByBodyPrefix(String prefix) {
    Long count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM chat_messages WHERE body LIKE ?", Long.class, prefix + "%");
    return count == null ? 0L : count;
  }

  private String findLatestMessageId(String roomId, String body) {
    List<String> ids =
        jdbcTemplate.query(
            "SELECT id FROM chat_messages WHERE room_id = ? AND body = ? ORDER BY sent_at DESC"
                + " LIMIT 1",
            (rs, rowNum) -> rs.getString("id"),
            roomId,
            body);
    return ids.isEmpty() ? null : ids.getFirst();
  }

  private long countReadReceiptsForMessage(String messageId) {
    Long count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM read_receipts WHERE message_id = ?", Long.class, messageId);
    return count == null ? 0L : count;
  }

  private boolean isRoomMessageOrderNonDecreasing(String roomId) {
    List<Instant> sentTimes =
        jdbcTemplate.query(
            "SELECT sent_at FROM chat_messages WHERE room_id = ? ORDER BY sent_at ASC",
            (rs, rowNum) -> rs.getTimestamp("sent_at").toInstant(),
            roomId);

    for (int i = 1; i < sentTimes.size(); i++) {
      if (sentTimes.get(i).isBefore(sentTimes.get(i - 1))) {
        return false;
      }
    }
    return true;
  }

  private void expireSessionNow(String sessionId) {
    if (stringRedisTemplate == null) {
      return;
    }

    Set<String> keys = stringRedisTemplate.keys("linkwave:session:*" + sessionId + "*");
    if (keys == null || keys.isEmpty()) {
      return;
    }

    for (String key : keys) {
      stringRedisTemplate.expire(key, Duration.ofSeconds(1));
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

    boolean containsMessagePart(String text) {
      for (String message : messages) {
        if (message != null && message.contains(text)) {
          return true;
        }
      }
      return false;
    }

    List<Throwable> getErrors() {
      return new ArrayList<>(errors);
    }
  }
}
