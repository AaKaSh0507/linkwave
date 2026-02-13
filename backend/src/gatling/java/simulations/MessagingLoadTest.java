package simulations;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.FeederBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.time.Duration;

/**
 * Messaging Load Test — validates message sending (via WebSocket), history retrieval (via REST),
 * and pagination under load.
 *
 * <p><b>Note:</b> There is no REST endpoint for sending messages. Messages are sent via the
 * WebSocket {@code chat.send} event through the native WebSocket handler at {@code /ws}. History
 * and pagination are tested via {@code GET /api/messages/{recipientId}}.
 */
public class MessagingLoadTest extends Simulation {

  private static final Config CONFIG = ConfigFactory.load();

  private static final String BASE_URL =
      CONFIG.hasPath("linkwave.baseUrl")
          ? CONFIG.getString("linkwave.baseUrl")
          : "http://localhost:8080";

  private static final String WS_URL =
      CONFIG.hasPath("linkwave.websocketUrl")
          ? CONFIG.getString("linkwave.websocketUrl")
          : "ws://localhost:8080/ws";

  private static final String FIXED_TEST_OTP =
      System.getenv().getOrDefault("TEST_OTP", System.getProperty("testOtp", "000000"));

  private static final HttpProtocolBuilder HTTP_PROTOCOL =
      http.baseUrl(BASE_URL)
          .wsBaseUrl(WS_URL)
          .acceptHeader("application/json")
          .contentTypeHeader("application/json")
          .disableCaching();

  private static final FeederBuilder.Batchable<String> USERS_SEND =
      csv("data/users.csv").circular();
  private static final FeederBuilder.Batchable<String> USERS_HISTORY =
      csv("data/users.csv").circular();
  private static final FeederBuilder.Batchable<String> USERS_PAGINATION =
      csv("data/users.csv").circular();

  private static Session setRecipient(Session session) {
    int userId = Integer.parseInt(session.getString("userId"));
    int recipientUserId = userId == 100 ? 1 : userId + 1;
    String recipientPhone = String.format("+155500%04d", recipientUserId);
    return session
        .set("recipientId", recipientPhone)
        .set("messageBody", "Load test message")
        .set("beforeTimestamp", java.time.Instant.now().toString());
  }

  // ── Shared Auth Chain ────────────────────────────────────────────────────
  private static ChainBuilder authenticateUser(FeederBuilder.Batchable<String> feeder) {
    return feed(feeder)
        .exec(session -> setRecipient(session))
        .exec(
            http("Msg: Request OTP")
                .post("/api/v1/auth/request-otp")
                .body(
                    StringBody(
                        "{\"phoneNumber\":\"#{phone}\",\"email\":\"msg-#{userId}@example.com\"}"))
                .asJson()
                .check(status().is(200)))
        .pause(Duration.ofSeconds(1))
        .exec(session -> session.set("otp", FIXED_TEST_OTP))
        .exec(
            http("Msg: Verify OTP")
                .post("/api/v1/auth/verify-otp")
                .body(StringBody("{\"phoneNumber\":\"#{phone}\",\"otp\":\"#{otp}\"}"))
                .asJson()
                .check(status().is(200))
                .check(jsonPath("$.authenticated").is("true"))
                .check(
                    headerRegex("Set-Cookie", "LINKWAVE_SESSION=([^;]+)").saveAs("sessionToken")))
        .exitHereIfFailed();
  }

  // ── Shared Room Creation ─────────────────────────────────────────────────
  private static final ChainBuilder CREATE_DIRECT_ROOM =
      exec(
          http("Msg: Create Direct Room")
              .post("/api/v1/chat/rooms/direct")
              .header("Cookie", "LINKWAVE_SESSION=#{sessionToken}")
              .body(StringBody("{\"otherUserPhone\":\"#{recipientId}\"}"))
              .asJson()
              .check(status().is(200))
              .check(jsonPath("$.id").saveAs("roomId")));

  // ── Scenario 1: Send Messages via WebSocket ─────────────────────────────
  private static final ChainBuilder SEND_MESSAGE_FLOW =
      authenticateUser(USERS_SEND)
          .exec(CREATE_DIRECT_ROOM)
          .exitHereIfFailed()
          // Connect WebSocket to send messages (the only way to send in this app)
          .exec(
              ws("Msg: WS Connect")
                  .connect("/ws")
                  .header("Cookie", "LINKWAVE_SESSION=#{sessionToken}")
                  .await(Duration.ofSeconds(10))
                  .on(ws.checkTextMessage("Msg: WS ACK").check(regex(".*connection\\.ack.*"))))
          .repeat(20, "msgIdx")
          .on(
              exec(ws("Msg: Send Chat Message")
                      .sendText(
                          "{\"event\":\"chat.send\",\"to\":\"#{roomId}\",\"payload\":{\"body\":\"#{messageBody}"
                              + " ##{msgIdx}\"}}")
                      .await(Duration.ofSeconds(10))
                      .on(
                          ws.checkTextMessage("Msg: Chat Sent ACK")
                              .check(regex(".*chat\\.sent.*"))))
                  .pause(1, 3))
          .exec(ws("Msg: WS Close").close());

  // ── Scenario 2: Get Message History via REST ─────────────────────────────
  private static final ChainBuilder GET_HISTORY_FLOW =
      authenticateUser(USERS_HISTORY)
          .exec(CREATE_DIRECT_ROOM)
          .exitHereIfFailed()
          .exec(
              http("Msg: Get History")
                  .get("/api/messages/#{recipientId}")
                  .header("Cookie", "LINKWAVE_SESSION=#{sessionToken}")
                  .check(status().is(200))
                  .check(jsonPath("$.messages").exists())
                  .check(jsonPath("$.hasMore").exists())
                  .check(jsonPath("$.oldestTimestamp").optional().saveAs("oldestTimestamp")))
          .pause(1, 2)
          // Also test room-scoped message endpoint
          .exec(
              http("Msg: Get Room Messages")
                  .get("/api/v1/chat/rooms/#{roomId}/messages")
                  .queryParam("page", "0")
                  .queryParam("size", "50")
                  .header("Cookie", "LINKWAVE_SESSION=#{sessionToken}")
                  .check(status().is(200)));

  // ── Scenario 3: Pagination ───────────────────────────────────────────────
  private static final ChainBuilder PAGINATION_FLOW =
      authenticateUser(USERS_PAGINATION)
          .exec(CREATE_DIRECT_ROOM)
          .exitHereIfFailed()
          .exec(
              http("Msg: First Page")
                  .get("/api/messages/#{recipientId}")
                  .header("Cookie", "LINKWAVE_SESSION=#{sessionToken}")
                  .check(status().is(200))
                  .check(jsonPath("$.hasMore").exists().saveAs("hasMore"))
                  .check(jsonPath("$.oldestTimestamp").optional().saveAs("oldestTimestamp")))
          .repeat(3, "pageNum")
          .on(
              doIf(session ->
                      session.contains("oldestTimestamp")
                          && session.getString("oldestTimestamp") != null
                          && !session.getString("oldestTimestamp").isBlank())
                  .then(
                      exec(
                          http("Msg: Next Page")
                              .get("/api/messages/#{recipientId}")
                              .queryParam("before", "#{oldestTimestamp}")
                              .header("Cookie", "LINKWAVE_SESSION=#{sessionToken}")
                              .check(status().is(200))
                              .check(jsonPath("$.hasMore").exists())
                              .check(
                                  jsonPath("$.oldestTimestamp")
                                      .optional()
                                      .saveAs("oldestTimestamp")))));

  // ── Scenarios ────────────────────────────────────────────────────────────
  private static final ScenarioBuilder SEND_MESSAGE_SCENARIO =
      scenario("Message Sending Load").exec(SEND_MESSAGE_FLOW);

  private static final ScenarioBuilder GET_HISTORY_SCENARIO =
      scenario("Message History Load").exec(GET_HISTORY_FLOW);

  private static final ScenarioBuilder PAGINATION_SCENARIO =
      scenario("Message Pagination Load").exec(PAGINATION_FLOW);

  // ── Load Profile ─────────────────────────────────────────────────────────
  {
    setUp(
            SEND_MESSAGE_SCENARIO.injectOpen(
                constantUsersPerSec(10).during(Duration.ofSeconds(60))),
            GET_HISTORY_SCENARIO.injectOpen(constantUsersPerSec(5).during(Duration.ofSeconds(60))),
            PAGINATION_SCENARIO.injectOpen(constantUsersPerSec(3).during(Duration.ofSeconds(60))))
        .protocols(HTTP_PROTOCOL)
        .assertions(
            global().successfulRequests().percent().gte(95.0),
            global().responseTime().max().lt(5000),
            global().responseTime().percentile3().lt(2000),
            details("Msg: Get History").responseTime().percentile3().lt(1000));
  }
}
