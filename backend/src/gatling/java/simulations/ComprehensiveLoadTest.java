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
 * Comprehensive Load Test — mixed workload simulating realistic user behaviour:
 *
 * <ul>
 *   <li>30% message senders (WebSocket {@code chat.send})
 *   <li>20% history readers (REST {@code GET /api/messages/{recipientId}})
 *   <li>30% WebSocket maintainers (heartbeats)
 *   <li>10% presence checkers (REST presence endpoints)
 *   <li>10% read-receipt senders (WebSocket {@code read.up_to})
 * </ul>
 *
 * <p>Total: 100 users ramped over 30 seconds.
 */
public class ComprehensiveLoadTest extends Simulation {

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

  // Separate feeders per scenario to avoid sharing
  private static final FeederBuilder.Batchable<String> USERS_MSG = csv("data/users.csv").circular();
  private static final FeederBuilder.Batchable<String> USERS_HIST =
      csv("data/users.csv").circular();
  private static final FeederBuilder.Batchable<String> USERS_WS = csv("data/users.csv").circular();
  private static final FeederBuilder.Batchable<String> USERS_PRESENCE =
      csv("data/users.csv").circular();
  private static final FeederBuilder.Batchable<String> USERS_READ =
      csv("data/users.csv").circular();

  private static Session setRecipient(Session session) {
    int userId = Integer.parseInt(session.getString("userId"));
    int recipientUserId = userId == 100 ? 1 : userId + 1;
    String recipientPhone = String.format("+155500%04d", recipientUserId);
    return session
        .set("recipientId", recipientPhone)
        .set("messageBody", "Comprehensive load test message");
  }

  // ── Shared Auth Chain ────────────────────────────────────────────────────
  private static ChainBuilder authenticateUser(FeederBuilder.Batchable<String> feeder) {
    return feed(feeder)
        .exec(session -> setRecipient(session))
        .exec(
            http("Comp: Request OTP")
                .post("/api/v1/auth/request-otp")
                .body(
                    StringBody(
                        "{\"phoneNumber\":\"#{phone}\",\"email\":\"comp-#{userId}@example.com\"}"))
                .asJson()
                .check(status().is(200)))
        .pause(Duration.ofSeconds(1))
        .exec(session -> session.set("otp", FIXED_TEST_OTP))
        .exec(
            http("Comp: Verify OTP")
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
          http("Comp: Create Direct Room")
              .post("/api/v1/chat/rooms/direct")
              .header("Cookie", "LINKWAVE_SESSION=#{sessionToken}")
              .body(StringBody("{\"otherUserPhone\":\"#{recipientId}\"}"))
              .asJson()
              .check(status().is(200))
              .check(jsonPath("$.id").saveAs("roomId")));

  // ── 30% — Message Senders (WebSocket) ────────────────────────────────────
  private static final ChainBuilder MESSAGE_SENDER_FLOW =
      authenticateUser(USERS_MSG)
          .exec(CREATE_DIRECT_ROOM)
          .exitHereIfFailed()
          .exec(
              ws("Comp: Sender WS Connect")
                  .connect("/ws")
                  .header("Cookie", "LINKWAVE_SESSION=#{sessionToken}")
                  .await(Duration.ofSeconds(10))
                  .on(ws.checkTextMessage("Comp: Sender ACK").check(regex(".*connection\\.ack.*"))))
          .repeat(10, "msgIdx")
          .on(
              exec(ws("Comp: Send Chat Message")
                      .sendText(
                          "{\"event\":\"chat.send\",\"to\":\"#{roomId}\",\"payload\":{\"body\":\"#{messageBody}"
                              + " ##{msgIdx}\"}}")
                      .await(Duration.ofSeconds(10))
                      .on(
                          ws.checkTextMessage("Comp: Chat Sent ACK")
                              .check(regex(".*chat\\.sent.*"))))
                  .pause(1, 2))
          .exec(ws("Comp: Sender WS Close").close());

  // ── 20% — History Readers (REST) ─────────────────────────────────────────
  private static final ChainBuilder HISTORY_FLOW =
      authenticateUser(USERS_HIST)
          .exec(
              http("Comp: Get History")
                  .get("/api/messages/#{recipientId}")
                  .header("Cookie", "LINKWAVE_SESSION=#{sessionToken}")
                  .check(status().is(200))
                  .check(jsonPath("$.messages").exists())
                  .check(jsonPath("$.oldestTimestamp").optional().saveAs("oldestTimestamp")))
          .pause(1, 3)
          .doIf(
              session ->
                  session.contains("oldestTimestamp")
                      && session.getString("oldestTimestamp") != null
                      && !session.getString("oldestTimestamp").isBlank())
          .then(
              exec(
                  http("Comp: Get History Page 2")
                      .get("/api/messages/#{recipientId}")
                      .queryParam("before", "#{oldestTimestamp}")
                      .header("Cookie", "LINKWAVE_SESSION=#{sessionToken}")
                      .check(status().is(200))));

  // ── 30% — WebSocket Connection Maintainers ───────────────────────────────
  private static final ChainBuilder WS_MAINTAIN_FLOW =
      authenticateUser(USERS_WS)
          .exec(
              ws("Comp: WS Connect")
                  .connect("/ws")
                  .header("Cookie", "LINKWAVE_SESSION=#{sessionToken}")
                  .await(Duration.ofSeconds(10))
                  .on(ws.checkTextMessage("Comp: WS ACK").check(regex(".*connection\\.ack.*"))))
          .repeat(15, "hbIdx")
          .on(
              exec(ws("Comp: Heartbeat")
                      .sendText("{\"event\":\"presence.heartbeat\"}")
                      .await(Duration.ofSeconds(5))
                      .on(
                          ws.checkTextMessage("Comp: HB ACK")
                              .check(regex(".*presence\\.heartbeat\\.ack.*"))))
                  .pause(Duration.ofSeconds(20)))
          .exec(ws("Comp: WS Close").close());

  // ── 10% — Presence Checkers (REST) ───────────────────────────────────────
  private static final ChainBuilder PRESENCE_FLOW =
      authenticateUser(USERS_PRESENCE)
          .exec(
              http("Comp: Get Presence")
                  .get("/api/v1/presence/#{phone}")
                  .header("Cookie", "LINKWAVE_SESSION=#{sessionToken}")
                  .check(status().in(200, 404)))
          .pause(1, 3)
          .exec(
              http("Comp: Bulk Presence")
                  .post("/api/v1/presence/bulk")
                  .header("Cookie", "LINKWAVE_SESSION=#{sessionToken}")
                  .body(StringBody("{\"userIds\":[\"#{phone}\",\"#{recipientId}\"]}"))
                  .asJson()
                  .check(status().is(200)));

  // ── 10% — Read Receipt Senders (WebSocket) ───────────────────────────────
  private static final ChainBuilder READ_RECEIPT_FLOW =
      authenticateUser(USERS_READ)
          .exec(CREATE_DIRECT_ROOM)
          .exitHereIfFailed()
          // Connect and send a message first, then mark as read
          .exec(
              ws("Comp: Read WS Connect")
                  .connect("/ws")
                  .header("Cookie", "LINKWAVE_SESSION=#{sessionToken}")
                  .await(Duration.ofSeconds(10))
                  .on(ws.checkTextMessage("Comp: Read ACK").check(regex(".*connection\\.ack.*"))))
          // Send a message to get a messageId
          .exec(
              ws("Comp: Read Send Message")
                  .sendText(
                      "{\"event\":\"chat.send\",\"to\":\"#{roomId}\",\"payload\":{\"body\":\"read"
                          + " receipt test\"}}")
                  .await(Duration.ofSeconds(10))
                  .on(
                      ws.checkTextMessage("Comp: Read Sent ACK")
                          .check(regex(".*chat\\.sent.*"))
                          .check(regex("\"messageId\":\"([^\"]+)\"").saveAs("messageId"))))
          .pause(Duration.ofSeconds(2))
          // Send read receipt
          .doIf(
              session ->
                  session.contains("messageId")
                      && session.getString("messageId") != null
                      && !session.getString("messageId").isBlank())
          .then(
              exec(
                  ws("Comp: Read Up To")
                      .sendText(
                          "{\"event\":\"read.up_to\",\"roomId\":\"#{roomId}\",\"messageId\":\"#{messageId}\"}")))
          .pause(Duration.ofSeconds(2))
          .exec(ws("Comp: Read WS Close").close());

  // ── Scenarios ────────────────────────────────────────────────────────────
  private static final ScenarioBuilder MSG_SENDER_SCENARIO =
      scenario("Comp: Message Senders (30%)").exec(MESSAGE_SENDER_FLOW);

  private static final ScenarioBuilder HISTORY_SCENARIO =
      scenario("Comp: History Readers (20%)").exec(HISTORY_FLOW);

  private static final ScenarioBuilder WS_MAINTAIN_SCENARIO =
      scenario("Comp: WS Maintainers (30%)").exec(WS_MAINTAIN_FLOW);

  private static final ScenarioBuilder PRESENCE_SCENARIO =
      scenario("Comp: Presence Checkers (10%)").exec(PRESENCE_FLOW);

  private static final ScenarioBuilder READ_RECEIPT_SCENARIO =
      scenario("Comp: Read Receipts (10%)").exec(READ_RECEIPT_FLOW);

  // ── Load Profile — 100 users total ───────────────────────────────────────
  {
    setUp(
            MSG_SENDER_SCENARIO.injectOpen(rampUsers(30).during(Duration.ofSeconds(30))),
            HISTORY_SCENARIO.injectOpen(rampUsers(20).during(Duration.ofSeconds(30))),
            WS_MAINTAIN_SCENARIO.injectOpen(rampUsers(30).during(Duration.ofSeconds(30))),
            PRESENCE_SCENARIO.injectOpen(rampUsers(10).during(Duration.ofSeconds(30))),
            READ_RECEIPT_SCENARIO.injectOpen(rampUsers(10).during(Duration.ofSeconds(30))))
        .protocols(HTTP_PROTOCOL)
        .assertions(
            global().successfulRequests().percent().gte(95.0),
            global().responseTime().max().lt(10000),
            global().responseTime().percentile3().lt(2000),
            details("Comp: Send Chat Message").responseTime().percentile3().lt(2000),
            details("Comp: Get History").responseTime().percentile3().lt(1000));
  }
}
