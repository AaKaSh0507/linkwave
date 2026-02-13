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
 * WebSocket Load Test — validates connection stability, message sending, receiving, and concurrent
 * connections under load.
 *
 * <p>Scenarios:
 *
 * <ol>
 *   <li>WS Connect &amp; Hold: 100 users connect, hold 5 min
 *   <li>WS Send Messages: 50 users send 10 chat messages each via {@code chat.send}
 *   <li>WS Concurrent: 100 users connect simultaneously
 *   <li>WS Reconnect: 20 users connect, disconnect, reconnect
 * </ol>
 */
public class WebSocketLoadTest extends Simulation {

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

  private static final FeederBuilder.Batchable<String> USERS_CONNECT =
      csv("data/users.csv").circular();
  private static final FeederBuilder.Batchable<String> USERS_SEND =
      csv("data/users.csv").circular();
  private static final FeederBuilder.Batchable<String> USERS_CONCURRENT =
      csv("data/users.csv").circular();
  private static final FeederBuilder.Batchable<String> USERS_RECONNECT =
      csv("data/users.csv").circular();

  private static Session setRecipient(Session session) {
    int userId = Integer.parseInt(session.getString("userId"));
    int recipientUserId = userId == 100 ? 1 : userId + 1;
    String recipientPhone = String.format("+155500%04d", recipientUserId);
    return session
        .set("recipientId", recipientPhone)
        .set("messageBody", "Load test msg from user " + userId);
  }

  // ── Shared Auth Chain ────────────────────────────────────────────────────
  private static ChainBuilder authenticateUser(FeederBuilder.Batchable<String> feeder) {
    return feed(feeder)
        .exec(
            http("WS: Request OTP")
                .post("/api/v1/auth/request-otp")
                .body(
                    StringBody(
                        "{\"phoneNumber\":\"#{phone}\",\"email\":\"ws-#{userId}@example.com\"}"))
                .asJson()
                .check(status().is(200)))
        .pause(Duration.ofSeconds(1))
        .exec(session -> session.set("otp", FIXED_TEST_OTP))
        .exec(
            http("WS: Verify OTP")
                .post("/api/v1/auth/verify-otp")
                .body(StringBody("{\"phoneNumber\":\"#{phone}\",\"otp\":\"#{otp}\"}"))
                .asJson()
                .check(status().is(200))
                .check(jsonPath("$.authenticated").is("true"))
                .check(
                    headerRegex("Set-Cookie", "LINKWAVE_SESSION=([^;]+)").saveAs("sessionToken")))
        .exitHereIfFailed();
  }

  // ── Scenario 1: Connect & Hold 5 minutes ────────────────────────────────
  private static final ChainBuilder WS_CONNECT_FLOW =
      authenticateUser(USERS_CONNECT)
          .exec(
              ws("WS Connect")
                  .connect("/ws")
                  .header("Cookie", "LINKWAVE_SESSION=#{sessionToken}")
                  .await(Duration.ofSeconds(10))
                  .on(ws.checkTextMessage("Connection ACK").check(regex(".*connection\\.ack.*"))))
          .exec(
              ws("WS Ping")
                  .sendText("{\"event\":\"ping\"}")
                  .await(Duration.ofSeconds(5))
                  .on(ws.checkTextMessage("Pong").check(regex(".*pong.*"))))
          .pause(Duration.ofSeconds(300))
          .exec(ws("WS Close").close());

  // ── Scenario 2: Send Messages via WebSocket ─────────────────────────────
  private static final ChainBuilder WS_SEND_FLOW =
      authenticateUser(USERS_SEND)
          .exec(session -> setRecipient(session))
          // First create a room to message into
          .exec(
              http("WS: Create Direct Room")
                  .post("/api/v1/chat/rooms/direct")
                  .header("Cookie", "LINKWAVE_SESSION=#{sessionToken}")
                  .body(StringBody("{\"otherUserPhone\":\"#{recipientId}\"}"))
                  .asJson()
                  .check(status().is(200))
                  .check(jsonPath("$.id").saveAs("roomId")))
          .exitHereIfFailed()
          .exec(
              ws("WS Connect Sender")
                  .connect("/ws")
                  .header("Cookie", "LINKWAVE_SESSION=#{sessionToken}")
                  .await(Duration.ofSeconds(10))
                  .on(
                      ws.checkTextMessage("Sender Connection ACK")
                          .check(regex(".*connection\\.ack.*"))))
          .repeat(10, "msgIdx")
          .on(
              exec(ws("WS Send Chat Message")
                      .sendText(
                          "{\"event\":\"chat.send\",\"to\":\"#{roomId}\",\"payload\":{\"body\":\"#{messageBody}"
                              + " ##{msgIdx}\"}}")
                      .await(Duration.ofSeconds(10))
                      .on(ws.checkTextMessage("Chat Sent ACK").check(regex(".*chat\\.sent.*"))))
                  .pause(2, 5))
          .exec(ws("WS Close Sender").close());

  // ── Scenario 3: Concurrent Connections ───────────────────────────────────
  private static final ChainBuilder WS_CONCURRENT_FLOW =
      authenticateUser(USERS_CONCURRENT)
          .exec(
              ws("WS Concurrent Connect")
                  .connect("/ws")
                  .header("Cookie", "LINKWAVE_SESSION=#{sessionToken}")
                  .await(Duration.ofSeconds(10))
                  .on(
                      ws.checkTextMessage("Concurrent Connection ACK")
                          .check(regex(".*connection\\.ack.*"))))
          // Maintain connection with periodic heartbeats
          .repeat(5, "hbIdx")
          .on(
              exec(ws("WS Concurrent Heartbeat")
                      .sendText("{\"event\":\"presence.heartbeat\"}")
                      .await(Duration.ofSeconds(5))
                      .on(
                          ws.checkTextMessage("Concurrent HB ACK")
                              .check(regex(".*presence\\.heartbeat\\.ack.*"))))
                  .pause(Duration.ofSeconds(60)))
          .exec(ws("WS Concurrent Close").close());

  // ── Scenario 4: Reconnect Resilience ─────────────────────────────────────
  private static final ChainBuilder WS_RECONNECT_FLOW =
      authenticateUser(USERS_RECONNECT)
          // First connection
          .exec(
              ws("WS Initial Connect")
                  .connect("/ws")
                  .header("Cookie", "LINKWAVE_SESSION=#{sessionToken}")
                  .await(Duration.ofSeconds(10))
                  .on(ws.checkTextMessage("Initial ACK").check(regex(".*connection\\.ack.*"))))
          .pause(Duration.ofSeconds(5))
          .exec(ws("WS Disconnect").close())
          .pause(Duration.ofSeconds(3))
          // Reconnect
          .exec(
              ws("WS Reconnect")
                  .connect("/ws")
                  .header("Cookie", "LINKWAVE_SESSION=#{sessionToken}")
                  .await(Duration.ofSeconds(10))
                  .on(ws.checkTextMessage("Reconnect ACK").check(regex(".*connection\\.ack.*"))))
          .exec(
              ws("WS Reconnect Ping")
                  .sendText("{\"event\":\"ping\"}")
                  .await(Duration.ofSeconds(5))
                  .on(ws.checkTextMessage("Reconnect Pong").check(regex(".*pong.*"))))
          .pause(Duration.ofSeconds(10))
          .exec(ws("WS Reconnect Close").close());

  // ── Scenarios ────────────────────────────────────────────────────────────
  private static final ScenarioBuilder WS_CONNECT_SCENARIO =
      scenario("WS Connect & Hold").exec(WS_CONNECT_FLOW);

  private static final ScenarioBuilder WS_SEND_SCENARIO =
      scenario("WS Send Messages").exec(WS_SEND_FLOW);

  private static final ScenarioBuilder WS_CONCURRENT_SCENARIO =
      scenario("WS Concurrent Connections").exec(WS_CONCURRENT_FLOW);

  private static final ScenarioBuilder WS_RECONNECT_SCENARIO =
      scenario("WS Reconnect Resilience").exec(WS_RECONNECT_FLOW);

  // ── Load Profile ─────────────────────────────────────────────────────────
  {
    setUp(
            WS_CONNECT_SCENARIO.injectOpen(rampUsers(100).during(Duration.ofSeconds(30))),
            WS_SEND_SCENARIO.injectOpen(rampUsers(50).during(Duration.ofSeconds(30))),
            WS_CONCURRENT_SCENARIO.injectOpen(atOnceUsers(100)),
            WS_RECONNECT_SCENARIO.injectOpen(rampUsers(20).during(Duration.ofSeconds(30))))
        .protocols(HTTP_PROTOCOL)
        .assertions(
            global().successfulRequests().percent().gte(95.0),
            global().responseTime().max().lt(10000),
            details("WS Connect").failedRequests().count().is(0L),
            details("WS Concurrent Connect").failedRequests().count().is(0L),
            details("WS Reconnect").failedRequests().count().is(0L));
  }
}
