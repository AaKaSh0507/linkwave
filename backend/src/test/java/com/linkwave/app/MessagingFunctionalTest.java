package com.linkwave.app;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.linkwave.app.domain.auth.OtpMetadata;
import com.linkwave.app.domain.chat.ChatMessage;
import com.linkwave.app.domain.chat.ChatMessageEntity;
import com.linkwave.app.service.auth.OtpService;
import com.linkwave.app.service.chat.ChatService;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

@Tag("integration")
class MessagingFunctionalTest extends FunctionalTestBase {

  private static final String AUTH_BASE = "/api/v1/auth";
  private static final String CHAT_BASE = "/api/v1/chat";
  private static final String SESSION_COOKIE = "LINKWAVE_SESSION";

  @Autowired private OtpService otpService;

  @Autowired private ChatService chatService;

  @Autowired private com.linkwave.app.repository.ChatMessageRepository chatMessageRepository;

  @Value("${spring.kafka.bootstrap-servers}")
  private String kafkaBootstrapServers;

  @Test
  @DisplayName("Message send flow succeeds and message is persisted with correct metadata")
  void testSendMessageSuccess() {
    String user1 = "+14155560101";
    String user2 = "+14155560102";

    String user1Session = authenticateUser(user1);
    authenticateUser(user2);

    String roomId = createDirectRoom(user1Session, user2);
    String body = "Hello from functional test";

    ChatMessage sent = chatService.sendMessage(roomId, user1, body);

    await()
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> chatMessageRepository.findById(sent.getMessageId()).isPresent());

    ChatMessageEntity persisted = chatMessageRepository.findById(sent.getMessageId()).orElseThrow();

    assertNotNull(sent.getMessageId());
    assertEquals(roomId, persisted.getRoom().getId());
    assertEquals(user1, persisted.getSenderPhone());
    assertEquals(body, persisted.getBody());
    assertNotNull(persisted.getSentAt());
  }

  @Test
  @DisplayName("Message persistence stores body, room and recent timestamp")
  void testMessagePersistence() {
    String user1 = "+14155560201";
    String user2 = "+14155560202";

    String user1Session = authenticateUser(user1);
    authenticateUser(user2);

    String roomId = createDirectRoom(user1Session, user2);
    String body = "Persistence validation message";
    Instant beforeSend = Instant.now();

    ChatMessage sent = chatService.sendMessage(roomId, user1, body);

    await()
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> chatMessageRepository.findById(sent.getMessageId()).isPresent());

    ChatMessageEntity persisted = chatMessageRepository.findById(sent.getMessageId()).orElseThrow();

    assertEquals(body, persisted.getBody());
    assertEquals(roomId, persisted.getRoom().getId());
    assertEquals(user1, persisted.getSenderPhone());

    Duration messageAge = Duration.between(beforeSend, persisted.getSentAt()).abs();
    assertTrue(messageAge.getSeconds() <= 5, "Persisted timestamp should be recent");
  }

  @Test
  @DisplayName("Kafka message flow publishes and consumes chat message")
  void testKafkaMessageFlow() {
    String user1 = "+14155560301";
    String user2 = "+14155560302";

    String user1Session = authenticateUser(user1);
    authenticateUser(user2);

    String roomId = createDirectRoom(user1Session, user2);
    String body = "Kafka flow test message";

    AtomicReference<String> consumedPayload = new AtomicReference<>();

    try (KafkaConsumer<String, String> consumer = createKafkaConsumer()) {
      consumer.subscribe(List.of("chat.messages"));
      consumer.poll(Duration.ofMillis(250));

      ChatMessage sent = chatService.sendMessage(roomId, user1, body);

      await()
          .atMost(5, TimeUnit.SECONDS)
          .until(
              () -> {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(250));
                for (ConsumerRecord<String, String> record : records) {
                  String value = record.value();
                  if (value != null && value.contains(sent.getMessageId())) {
                    consumedPayload.set(value);
                    return true;
                  }
                }
                return false;
              });

      assertNotNull(consumedPayload.get(), "Expected chat message to be consumed from Kafka");
      assertTrue(consumedPayload.get().contains(body));

      await()
          .atMost(5, TimeUnit.SECONDS)
          .until(() -> chatMessageRepository.findById(sent.getMessageId()).isPresent());
    }
  }

  @Test
  @DisplayName("Message history returns 10 newest-first messages")
  void testMessageHistorySuccess() {
    String user1 = "+14155560401";
    String user2 = "+14155560402";

    String user1Session = authenticateUser(user1);
    String user2Session = authenticateUser(user2);

    String roomId = createDirectRoom(user1Session, user2);

    List<String> expectedBodies = new ArrayList<>();
    for (int i = 1; i <= 10; i++) {
      String body = "history-message-" + i;
      expectedBodies.add(body);
      chatService.sendMessage(roomId, user1, body);
    }

    await()
        .atMost(5, TimeUnit.SECONDS)
        .until(
            () ->
                ((Number)
                            jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM chat_messages WHERE room_id = ?",
                                Long.class,
                                roomId))
                        .longValue()
                    >= 10L);

    Response response =
        given()
            .cookie(SESSION_COOKIE, user2Session)
            .when()
            .get("/api/messages/{recipientId}", user1)
            .then()
            .statusCode(200)
            .extract()
            .response();

    List<Map<String, Object>> messages = response.jsonPath().getList("messages");
    assertEquals(10, messages.size());

    Instant previous = Instant.MAX;
    for (Map<String, Object> message : messages) {
      Instant current = Instant.parse(message.get("sentAt").toString());
      assertTrue(!current.isAfter(previous), "Messages should be newest first");
      previous = current;

      assertEquals(user1, message.get("senderPhone"));
      assertTrue(expectedBodies.contains(message.get("body")));
    }
  }

  @Test
  @DisplayName("Message history supports pagination across 60 messages")
  void testMessageHistoryPagination() {
    String user1 = "+14155560501";
    String user2 = "+14155560502";

    String user1Session = authenticateUser(user1);
    String user2Session = authenticateUser(user2);

    String roomId = createDirectRoom(user1Session, user2);

    for (int i = 1; i <= 60; i++) {
      chatService.sendMessage(roomId, user1, "pagination-message-" + i);
    }

    await()
        .atMost(5, TimeUnit.SECONDS)
        .until(
            () ->
                ((Number)
                            jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM chat_messages WHERE room_id = ?",
                                Long.class,
                                roomId))
                        .longValue()
                    >= 60L);

    Response firstPage =
        given()
            .cookie(SESSION_COOKIE, user2Session)
            .when()
            .get("/api/messages/{recipientId}", user1)
            .then()
            .statusCode(200)
            .extract()
            .response();

    List<Map<String, Object>> firstMessages = firstPage.jsonPath().getList("messages");
    assertEquals(50, firstMessages.size());
    assertTrue(firstPage.jsonPath().getBoolean("hasMore"));

    String oldestTimestamp = firstPage.jsonPath().getString("oldestTimestamp");
    assertNotNull(oldestTimestamp);

    Response secondPage =
        given()
            .cookie(SESSION_COOKIE, user2Session)
            .queryParam("before", oldestTimestamp)
            .when()
            .get("/api/messages/{recipientId}", user1)
            .then()
            .statusCode(200)
            .extract()
            .response();

    List<Map<String, Object>> secondMessages = secondPage.jsonPath().getList("messages");
    assertEquals(10, secondMessages.size());
    assertEquals(false, secondPage.jsonPath().getBoolean("hasMore"));
  }

  private String authenticateUser(String phone) {
    String email = "msg-auth-" + phone.substring(phone.length() - 4) + "@example.com";

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

  private KafkaConsumer<String, String> createKafkaConsumer() {
    Properties props = new Properties();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "functional-test-" + UUID.randomUUID());
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

    return new KafkaConsumer<>(props);
  }

  @Test
  @DisplayName("Send message to non-existent room returns error")
  void testSendMessage_InvalidRoom() {
    String user1 = "+14155560601";
    authenticateUser(user1);

    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> chatService.sendMessage("non-existent-room-id", user1, "Should fail"));
  }

  @Test
  @DisplayName("Send message by non-member of room is rejected")
  void testSendMessage_NonMember() {
    String user1 = "+14155560701";
    String user2 = "+14155560702";
    String outsider = "+14155560703";

    String user1Session = authenticateUser(user1);
    authenticateUser(user2);
    authenticateUser(outsider);

    String roomId = createDirectRoom(user1Session, user2);

    org.junit.jupiter.api.Assertions.assertThrows(
        SecurityException.class,
        () -> chatService.sendMessage(roomId, outsider, "Intruder message"));
  }

  @Test
  @DisplayName("Message history without authentication returns 401")
  void testMessageHistory_Unauthorized() {
    given().when().get("/api/messages/{recipientId}", "+14155560801").then().statusCode(401);
  }

  @Test
  @DisplayName("Group room creation and messaging flow succeeds")
  void testGroupRoomMessaging() {
    String user1 = "+14155560901";
    String user2 = "+14155560902";
    String user3 = "+14155560903";

    String user1Session = authenticateUser(user1);
    authenticateUser(user2);
    authenticateUser(user3);

    Response response =
        given()
            .cookie(SESSION_COOKIE, user1Session)
            .contentType(ContentType.JSON)
            .body(Map.of("name", "Test Group", "members", List.of(user1, user2, user3)))
            .when()
            .post(CHAT_BASE + "/rooms/group")
            .then()
            .statusCode(200)
            .extract()
            .response();

    String groupRoomId = response.jsonPath().getString("id");
    assertNotNull(groupRoomId);
    assertEquals("GROUP", response.jsonPath().getString("type"));

    chatService.sendMessage(groupRoomId, user1, "Hello group!");

    await()
        .atMost(5, TimeUnit.SECONDS)
        .until(
            () ->
                ((Number)
                            jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM chat_messages WHERE room_id = ?",
                                Long.class,
                                groupRoomId))
                        .longValue()
                    >= 1L);

    ChatMessageEntity persisted =
        chatMessageRepository
            .findById(
                jdbcTemplate.queryForObject(
                    "SELECT id FROM chat_messages WHERE room_id = ? LIMIT 1",
                    String.class,
                    groupRoomId))
            .orElseThrow();

    assertEquals("Hello group!", persisted.getBody());
    assertEquals(user1, persisted.getSenderPhone());
  }

  @Test
  @DisplayName("User room listing returns all rooms the user belongs to")
  void testRoomListing() {
    String user1 = "+14155561001";
    String user2 = "+14155561002";
    String user3 = "+14155561003";

    String user1Session = authenticateUser(user1);
    authenticateUser(user2);
    authenticateUser(user3);

    createDirectRoom(user1Session, user2);
    createDirectRoom(user1Session, user3);

    Response response =
        given()
            .cookie(SESSION_COOKIE, user1Session)
            .when()
            .get(CHAT_BASE + "/rooms")
            .then()
            .statusCode(200)
            .extract()
            .response();

    List<Map<String, Object>> rooms = response.jsonPath().getList("");
    assertTrue(rooms.size() >= 2, "User should have at least 2 rooms");
  }

  @Test
  @DisplayName("Room members endpoint returns correct member list")
  void testRoomMembers() {
    String user1 = "+14155561101";
    String user2 = "+14155561102";

    String user1Session = authenticateUser(user1);
    authenticateUser(user2);

    String roomId = createDirectRoom(user1Session, user2);

    Response response =
        given()
            .cookie(SESSION_COOKIE, user1Session)
            .when()
            .get(CHAT_BASE + "/rooms/{roomId}/members", roomId)
            .then()
            .statusCode(200)
            .extract()
            .response();

    List<Map<String, Object>> members = response.jsonPath().getList("");
    assertEquals(2, members.size());

    List<String> phoneNumbers = members.stream().map(m -> (String) m.get("phoneNumber")).toList();
    assertTrue(phoneNumbers.contains(user1));
    assertTrue(phoneNumbers.contains(user2));
  }

  @Test
  @DisplayName("Room messages endpoint with pagination returns correct page")
  void testRoomMessagesEndpoint() {
    String user1 = "+14155561201";
    String user2 = "+14155561202";

    String user1Session = authenticateUser(user1);
    authenticateUser(user2);

    String roomId = createDirectRoom(user1Session, user2);

    for (int i = 1; i <= 5; i++) {
      chatService.sendMessage(roomId, user1, "room-endpoint-msg-" + i);
    }

    await()
        .atMost(5, TimeUnit.SECONDS)
        .until(
            () ->
                ((Number)
                            jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM chat_messages WHERE room_id = ?",
                                Long.class,
                                roomId))
                        .longValue()
                    >= 5L);

    Response response =
        given()
            .cookie(SESSION_COOKIE, user1Session)
            .queryParam("page", 0)
            .queryParam("size", 3)
            .when()
            .get(CHAT_BASE + "/rooms/{roomId}/messages", roomId)
            .then()
            .statusCode(200)
            .extract()
            .response();

    List<Map<String, Object>> messages = response.jsonPath().getList("messages");
    assertEquals(3, messages.size());
    assertTrue(response.jsonPath().getLong("total") >= 5);
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
}
