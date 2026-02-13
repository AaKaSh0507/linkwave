package com.linkwave.app;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.linkwave.app.domain.auth.OtpMetadata;
import com.linkwave.app.domain.auth.ThrottleMetadata;
import com.linkwave.app.domain.chat.ChatMemberEntity;
import com.linkwave.app.domain.chat.ChatRoomEntity;
import com.linkwave.app.repository.ChatMemberRepository;
import com.linkwave.app.repository.ChatRoomRepository;
import com.linkwave.app.service.auth.EmailService;
import com.linkwave.app.service.auth.OtpService;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@TestPropertySource(properties = {"linkwave.auth.otp.throttle-max-requests=1"})
@Tag("integration")
class AuthenticationFunctionalTest extends FunctionalTestBase {

  private static final String AUTH_BASE = "/api/v1/auth";
  private static final String USER_BASE = "/api/v1/user";
  private static final String SESSION_COOKIE = "LINKWAVE_SESSION";

  @Autowired private OtpService otpService;

  @Autowired private ChatRoomRepository chatRoomRepository;

  @Autowired private ChatMemberRepository chatMemberRepository;

  @MockitoSpyBean private EmailService emailService;

  @Autowired(required = false)
  private StringRedisTemplate redisTemplate;

  @BeforeEach
  void resetInternalStoresAndSpies() {
    clearOtpAndThrottleStores();
    reset(emailService);
  }

  @Test
  @DisplayName("OTP request succeeds and stores OTP metadata with expected TTL")
  void testOtpRequestSuccess() {
    String phone = "+14155550101";
    String email = "otp-success@example.com";

    Response response =
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("phoneNumber", phone, "email", email))
            .when()
            .post(AUTH_BASE + "/request-otp")
            .then()
            .statusCode(200)
            .extract()
            .response();

    assertEquals("OTP sent successfully", response.jsonPath().getString("message"));

    OtpMetadata metadata = getOtpStore().get(phone);
    assertNotNull(metadata, "OTP metadata should exist after request");

    long ttlSeconds = Duration.between(Instant.now(), metadata.getExpiresAt()).toSeconds();
    assertTrue(ttlSeconds <= 300 && ttlSeconds >= 290, "OTP TTL should be close to 5 minutes");

    verify(emailService, timeout(1000).times(1))
        .sendOtpEmail(eq(email), eq(metadata.getOtpValue()));
  }

  @Test
  @DisplayName("OTP request is throttled for repeated requests from same phone")
  void testOtpRequestThrottled() throws InterruptedException {
    String phone = "+14155550102";
    String email = "otp-throttle@example.com";

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("phoneNumber", phone, "email", email))
        .when()
        .post(AUTH_BASE + "/request-otp")
        .then()
        .statusCode(200);

    Thread.sleep(1000);

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("phoneNumber", phone, "email", email))
        .when()
        .post(AUTH_BASE + "/request-otp")
        .then()
        .statusCode(429)
        .body("error", containsString("Too many OTP requests"));

    ThrottleMetadata throttleMetadata = getThrottleStore().get(phone);
    assertNotNull(throttleMetadata, "Throttle metadata should exist for throttled phone");
    assertFalse(
        throttleMetadata.getRequestTimestamps().isEmpty(),
        "Throttle metadata should include request timestamps");
  }

  @Test
  @DisplayName("OTP verification succeeds and creates authenticated session")
  void testOtpVerificationSuccess() {
    String phone = "+14155550103";
    String email = "otp-verify-success@example.com";

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("phoneNumber", phone, "email", email))
        .when()
        .post(AUTH_BASE + "/request-otp")
        .then()
        .statusCode(200);

    OtpMetadata metadata = getOtpStore().get(phone);
    assertNotNull(metadata, "OTP should be present before verification");

    Response verifyResponse =
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("phoneNumber", phone, "otp", metadata.getOtpValue()))
            .when()
            .post(AUTH_BASE + "/verify-otp")
            .then()
            .statusCode(200)
            .extract()
            .response();

    assertTrue(verifyResponse.jsonPath().getBoolean("authenticated"));
    assertEquals("Authentication successful", verifyResponse.jsonPath().getString("message"));

    String sessionId = verifyResponse.getCookie(SESSION_COOKIE);
    assertNotNull(sessionId, "Session cookie should be set after successful verification");

    assertNull(getOtpStore().get(phone), "OTP must be deleted after successful verification");

    assertTrue(hasSessionState(sessionId), "Session should be persisted after authentication");
  }

  @Test
  @DisplayName("OTP verification fails for invalid OTP and does not create session")
  void testOtpVerificationInvalidOtp() {
    String phone = "+14155550104";
    String email = "otp-invalid@example.com";

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("phoneNumber", phone, "email", email))
        .when()
        .post(AUTH_BASE + "/request-otp")
        .then()
        .statusCode(200);

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("phoneNumber", phone, "otp", "000000"))
        .when()
        .post(AUTH_BASE + "/verify-otp")
        .then()
        .statusCode(401)
        .body("error", containsString("Invalid OTP"));

    assertNotNull(getOtpStore().get(phone), "OTP should remain for invalid verification attempts");

    given().when().get(USER_BASE + "/me").then().statusCode(401);
  }

  @Test
  @DisplayName("OTP verification fails when OTP is expired")
  void testOtpVerificationExpired() {
    String phone = "+14155550105";
    String email = "otp-expired@example.com";

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("phoneNumber", phone, "email", email))
        .when()
        .post(AUTH_BASE + "/request-otp")
        .then()
        .statusCode(200);

    OtpMetadata current = getOtpStore().get(phone);
    assertNotNull(current, "OTP should exist before forcing expiry");

    getOtpStore()
        .put(
            phone,
            new OtpMetadata(
                current.getOtpValue(), current.getCreatedAt(), Instant.now().minusSeconds(1)));

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("phoneNumber", phone, "otp", current.getOtpValue()))
        .when()
        .post(AUTH_BASE + "/verify-otp")
        .then()
        .statusCode(401)
        .body("error", containsString("expired"));
  }

  @Test
  @DisplayName("Authenticated session can access protected message history endpoint")
  void testSessionAuthenticationSuccess() {
    String phone = "+14155550106";
    String recipientPhone = "+14155550999";
    String sessionId = authenticateUser(phone);

    createDirectConversation(phone, recipientPhone);

    given()
        .cookie(SESSION_COOKIE, sessionId)
        .when()
        .get("/api/messages/{recipientId}", recipientPhone)
        .then()
        .statusCode(200);
  }

  @Test
  @DisplayName("Expired session is rejected for protected endpoints")
  void testSessionAuthenticationExpired() throws InterruptedException {
    String phone = "+14155550107";
    String recipientPhone = "+14155550888";
    String sessionId = authenticateUser(phone);

    createDirectConversation(phone, recipientPhone);
    expireSessionNow(sessionId);

    Thread.sleep(1500);

    given()
        .cookie(SESSION_COOKIE, sessionId)
        .when()
        .get("/api/messages/{recipientId}", recipientPhone)
        .then()
        .statusCode(401)
        .body(containsString("Unauthorized"));
  }

  protected String authenticateUser(String phone) {
    String email = "auth-" + phone.substring(phone.length() - 4) + "@example.com";

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

  private void createDirectConversation(String userPhone, String recipientPhone) {
    ChatRoomEntity room = new ChatRoomEntity();
    room.setId(UUID.randomUUID().toString());
    room.setRoomType(ChatRoomEntity.RoomType.DIRECT);
    room.setCreatedAt(Instant.now());
    room.setUpdatedAt(Instant.now());

    ChatRoomEntity savedRoom = chatRoomRepository.save(room);

    chatMemberRepository.save(new ChatMemberEntity(savedRoom, userPhone, Instant.now()));
    chatMemberRepository.save(new ChatMemberEntity(savedRoom, recipientPhone, Instant.now()));
  }

  private void clearOtpAndThrottleStores() {
    getOtpStore().clear();
    getThrottleStore().clear();
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

  @SuppressWarnings("unchecked")
  private Map<String, ThrottleMetadata> getThrottleStore() {
    try {
      Field field = OtpService.class.getDeclaredField("throttleStore");
      field.setAccessible(true);
      return (Map<String, ThrottleMetadata>) field.get(otpService);
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException("Unable to access throttleStore for functional tests", ex);
    }
  }

  private boolean hasSessionState(String sessionId) {
    if (redisTemplate == null) {
      return !sessionId.isBlank();
    }

    Set<String> keys = redisTemplate.keys("linkwave:session:*" + sessionId + "*");
    return keys != null && !keys.isEmpty();
  }

  @Test
  @DisplayName("OTP request fails with invalid phone number format")
  void testOtpRequest_InvalidPhoneNumber() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("phoneNumber", "not-a-phone", "email", "valid@example.com"))
        .when()
        .post(AUTH_BASE + "/request-otp")
        .then()
        .statusCode(400);
  }

  @Test
  @DisplayName("OTP request fails with invalid email format")
  void testOtpRequest_InvalidEmail() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("phoneNumber", "+14155550200", "email", "not-an-email"))
        .when()
        .post(AUTH_BASE + "/request-otp")
        .then()
        .statusCode(400);
  }

  @Test
  @DisplayName("OTP request fails with missing required fields")
  void testOtpRequest_MissingFields() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of())
        .when()
        .post(AUTH_BASE + "/request-otp")
        .then()
        .statusCode(400);

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("phoneNumber", "+14155550201"))
        .when()
        .post(AUTH_BASE + "/request-otp")
        .then()
        .statusCode(400);
  }

  @Test
  @DisplayName("OTP verification fails when no OTP was requested")
  void testOtpVerification_NoOtpRequested() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("phoneNumber", "+14155550300", "otp", "123456"))
        .when()
        .post(AUTH_BASE + "/verify-otp")
        .then()
        .statusCode(401)
        .body("error", containsString("No OTP found"));
  }

  @Test
  @DisplayName("Logout invalidates session and blocks subsequent authenticated requests")
  void testLogoutSuccess() {
    String phone = "+14155550108";
    String recipientPhone = "+14155550777";
    String sessionId = authenticateUser(phone);

    createDirectConversation(phone, recipientPhone);

    given()
        .cookie(SESSION_COOKIE, sessionId)
        .when()
        .get("/api/messages/{recipientId}", recipientPhone)
        .then()
        .statusCode(200);

    given()
        .cookie(SESSION_COOKIE, sessionId)
        .when()
        .post(AUTH_BASE + "/logout")
        .then()
        .statusCode(200);

    given()
        .cookie(SESSION_COOKIE, sessionId)
        .when()
        .get("/api/messages/{recipientId}", recipientPhone)
        .then()
        .statusCode(401);
  }

  @Test
  @DisplayName("CSRF token endpoint is accessible without authentication")
  void testCsrfTokenEndpoint() {
    given().when().get(AUTH_BASE + "/csrf").then().statusCode(200);
  }

  private void expireSessionNow(String sessionId) {
    if (redisTemplate == null) {
      return;
    }

    Set<String> keys = redisTemplate.keys("linkwave:session:*" + sessionId + "*");
    if (keys == null || keys.isEmpty()) {
      return;
    }

    for (String key : keys) {
      redisTemplate.expire(key, Duration.ofSeconds(1));
    }
  }
}
