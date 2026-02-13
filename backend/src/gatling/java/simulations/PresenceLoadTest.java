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
 * Presence &amp; Typing Load Test — validates heartbeat, presence tracking, and typing indicators
 * under load.
 *
 * <p>Scenarios:
 *
 * <ol>
 *   <li>Heartbeat: 100 users send 15 heartbeats every 20s
 *   <li>Presence REST: 50 users query single + bulk presence via REST
 *   <li>Typing Indicators: 20 users send typing.start/stop
 * </ol>
 */
public class PresenceLoadTest extends Simulation {

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

  private static final FeederBuilder.Batchable<String> USERS_HB = csv("data/users.csv").circular();
  private static final FeederBuilder.Batchable<String> USERS_PRESENCE =
      csv("data/users.csv").circular();
  private static final FeederBuilder.Batchable<String> USERS_TYPING =
      csv("data/users.csv").circular();

  private static Session setRecipient(Session session) {
    int userId = Integer.parseInt(session.getString("userId"));
    int recipientUserId = userId == 100 ? 1 : userId + 1;
    String recipientPhone = String.format("+155500%04d", recipientUserId);
    return session.set("recipientId", recipientPhone);
  }

  // ── Shared Auth Chain ────────────────────────────────────────────────────
  private static ChainBuilder authenticateUser(FeederBuilder.Batchable<String> feeder) {
    return feed(feeder)
        .exec(session -> setRecipient(session))
        .exec(
            http("Presence: Request OTP")
                .post("/api/v1/auth/request-otp")
                .body(
                    StringBody(
                        "{\"phoneNumber\":\"#{phone}\",\"email\":\"presence-#{userId}@example.com\"}"))
                .asJson()
                .check(status().is(200)))
        .pause(Duration.ofSeconds(1))
        .exec(session -> session.set("otp", FIXED_TEST_OTP))
        .exec(
            http("Presence: Verify OTP")
                .post("/api/v1/auth/verify-otp")
                .body(StringBody("{\"phoneNumber\":\"#{phone}\",\"otp\":\"#{otp}\"}"))
                .asJson()
                .check(status().is(200))
                .check(jsonPath("$.authenticated").is("true"))
                .check(
                    headerRegex("Set-Cookie", "LINKWAVE_SESSION=([^;]+)").saveAs("sessionToken")))
        .exitHereIfFailed();
  }

  // ── Scenario 1: WebSocket Heartbeats ─────────────────────────────────────
  private static final ChainBuilder HEARTBEAT_FLOW =
      authenticateUser(USERS_HB)
          .exec(
              ws("Presence: WS Connect")
                  .connect("/ws")
                  .header("Cookie", "LINKWAVE_SESSION=#{sessionToken}")
                  .await(Duration.ofSeconds(10))
                  .on(ws.checkTextMessage("Presence: WS ACK").check(regex(".*connection\\.ack.*"))))
          .repeat(15, "hbIdx")
          .on(
              exec(ws("Presence: Heartbeat")
                      .sendText("{\"event\":\"presence.heartbeat\"}")
                      .await(Duration.ofSeconds(5))
                      .on(
                          ws.checkTextMessage("Presence: HB ACK")
                              .check(regex(".*presence\\.heartbeat\\.ack.*"))))
                  .pause(Duration.ofSeconds(20)))
          .exec(ws("Presence: WS Close").close());

  // ── Scenario 2: Presence REST Queries ────────────────────────────────────
  private static final ChainBuilder PRESENCE_REST_FLOW =
      authenticateUser(USERS_PRESENCE)
          // Query single-user presence
          .exec(
              http("Presence: Get Single")
                  .get("/api/v1/presence/#{phone}")
                  .header("Cookie", "LINKWAVE_SESSION=#{sessionToken}")
                  .check(status().in(200, 404)))
          .pause(1, 3)
          // Query bulk presence
          .exec(
              http("Presence: Get Bulk")
                  .post("/api/v1/presence/bulk")
                  .header("Cookie", "LINKWAVE_SESSION=#{sessionToken}")
                  .body(StringBody("{\"userIds\":[\"#{phone}\",\"#{recipientId}\"]}"))
                  .asJson()
                  .check(status().is(200)))
          .pause(1, 2)
          // Check Prometheus metrics endpoint
          .exec(
              http("Presence: Metrics")
                  .get("/actuator/metrics/presence.updates.total")
                  .check(status().in(200, 404)));

  // ── Scenario 3: Typing Indicators ────────────────────────────────────────
  private static final ChainBuilder TYPING_FLOW =
      authenticateUser(USERS_TYPING)
          // Create a room to type in
          .exec(
              http("Typing: Create Room")
                  .post("/api/v1/chat/rooms/direct")
                  .header("Cookie", "LINKWAVE_SESSION=#{sessionToken}")
                  .body(StringBody("{\"otherUserPhone\":\"#{recipientId}\"}"))
                  .asJson()
                  .check(status().is(200))
                  .check(jsonPath("$.id").saveAs("roomId")))
          .exitHereIfFailed()
          .exec(
              ws("Typing: WS Connect")
                  .connect("/ws")
                  .header("Cookie", "LINKWAVE_SESSION=#{sessionToken}")
                  .await(Duration.ofSeconds(10))
                  .on(ws.checkTextMessage("Typing: WS ACK").check(regex(".*connection\\.ack.*"))))
          // Send typing.start
          .exec(
              ws("Typing: Start").sendText("{\"event\":\"typing.start\",\"roomId\":\"#{roomId}\"}"))
          .pause(Duration.ofSeconds(3))
          // Send typing.stop
          .exec(ws("Typing: Stop").sendText("{\"event\":\"typing.stop\",\"roomId\":\"#{roomId}\"}"))
          .pause(Duration.ofSeconds(2))
          // Another cycle of typing
          .exec(
              ws("Typing: Start 2")
                  .sendText("{\"event\":\"typing.start\",\"roomId\":\"#{roomId}\"}"))
          .pause(Duration.ofSeconds(5))
          .exec(
              ws("Typing: Stop 2").sendText("{\"event\":\"typing.stop\",\"roomId\":\"#{roomId}\"}"))
          .exec(ws("Typing: WS Close").close());

  // ── Scenarios ────────────────────────────────────────────────────────────
  private static final ScenarioBuilder HEARTBEAT_SCENARIO =
      scenario("Presence Heartbeat Load").exec(HEARTBEAT_FLOW);

  private static final ScenarioBuilder PRESENCE_REST_SCENARIO =
      scenario("Presence REST Query Load").exec(PRESENCE_REST_FLOW);

  private static final ScenarioBuilder TYPING_SCENARIO =
      scenario("Typing Indicator Load").exec(TYPING_FLOW);

  // ── Load Profile ─────────────────────────────────────────────────────────
  {
    setUp(
            HEARTBEAT_SCENARIO.injectOpen(rampUsers(100).during(Duration.ofSeconds(30))),
            PRESENCE_REST_SCENARIO.injectOpen(rampUsers(50).during(Duration.ofSeconds(30))),
            TYPING_SCENARIO.injectOpen(rampUsers(20).during(Duration.ofSeconds(30))))
        .protocols(HTTP_PROTOCOL)
        .assertions(
            global().successfulRequests().percent().gte(95.0),
            global().responseTime().max().lt(5000),
            details("Presence: Heartbeat").failedRequests().count().is(0L));
  }
}
