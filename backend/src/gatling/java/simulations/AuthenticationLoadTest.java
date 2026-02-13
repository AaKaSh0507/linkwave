package simulations;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.time.Duration;

public abstract class AuthenticationLoadTest extends Simulation {

  private static final Config CONFIG = ConfigFactory.load();

  private static final String BASE_URL =
      CONFIG.hasPath("linkwave.baseUrl")
          ? CONFIG.getString("linkwave.baseUrl")
          : "http://localhost:8080";

  private static final String FIXED_TEST_OTP =
      System.getenv().getOrDefault("TEST_OTP", System.getProperty("testOtp", "000000"));

  private static final HttpProtocolBuilder HTTP_PROTOCOL =
      http.baseUrl(BASE_URL)
          .acceptHeader("application/json")
          .contentTypeHeader("application/json")
          .disableCaching();

  private static final FeederBuilder.Batchable<String> USERS_OTP_REQ =
      csv("data/users.csv").circular();
  private static final FeederBuilder.Batchable<String> USERS_OTP_VERIFY =
      csv("data/users.csv").circular();
  private static final FeederBuilder.Batchable<String> USERS_AUTH =
      csv("data/users.csv").circular();
  private static final ChainBuilder REQUEST_OTP_FLOW =
      feed(USERS_OTP_REQ)
          .exec(
              http("Request OTP")
                  .post("/api/v1/auth/request-otp")
                  .body(
                      StringBody(
                          "{\"phoneNumber\":\"#{phone}\",\"email\":\"load-#{userId}@example.com\"}"))
                  .asJson()
                  .check(status().is(200))
                  .check(jsonPath("$.message").is("OTP sent successfully")))
          .pause(1, 5);
  private static final ChainBuilder VERIFY_OTP_FLOW =
      feed(USERS_OTP_VERIFY)
          .exec(
              http("Verify: Request OTP")
                  .post("/api/v1/auth/request-otp")
                  .body(
                      StringBody(
                          "{\"phoneNumber\":\"#{phone}\",\"email\":\"load-#{userId}@example.com\"}"))
                  .asJson()
                  .check(status().is(200)))
          .pause(Duration.ofSeconds(1))
          .exec(session -> session.set("otp", FIXED_TEST_OTP))
          .exec(
              http("Verify OTP")
                  .post("/api/v1/auth/verify-otp")
                  .body(StringBody("{\"phoneNumber\":\"#{phone}\",\"otp\":\"#{otp}\"}"))
                  .asJson()
                  .check(status().is(200))
                  .check(jsonPath("$.authenticated").is("true"))
                  .check(
                      headerRegex("Set-Cookie", "LINKWAVE_SESSION=([^;]+)").saveAs("sessionToken")))
          .exec(
              session -> {
                String token = session.getString("sessionToken");
                if (token == null || token.isBlank()) {
                  throw new IllegalStateException(
                      "No session token obtained after OTP verification");
                }
                return session;
              })
          .pause(1, 5);
  private static final ChainBuilder AUTHENTICATED_REQUEST_FLOW =
      feed(USERS_AUTH)
          .exec(
              http("Auth: Request OTP")
                  .post("/api/v1/auth/request-otp")
                  .body(
                      StringBody(
                          "{\"phoneNumber\":\"#{phone}\",\"email\":\"load-#{userId}@example.com\"}"))
                  .asJson()
                  .check(status().is(200)))
          .pause(Duration.ofSeconds(1))
          .exec(session -> session.set("otp", FIXED_TEST_OTP))
          .exec(
              http("Auth: Verify OTP")
                  .post("/api/v1/auth/verify-otp")
                  .body(StringBody("{\"phoneNumber\":\"#{phone}\",\"otp\":\"#{otp}\"}"))
                  .asJson()
                  .check(status().is(200))
                  .check(jsonPath("$.authenticated").is("true"))
                  .check(
                      headerRegex("Set-Cookie", "LINKWAVE_SESSION=([^;]+)").saveAs("sessionToken")))
          .exitHereIfFailed()
          .exec(
              http("Get User Profile")
                  .get("/api/v1/user/me")
                  .header("Cookie", "LINKWAVE_SESSION=#{sessionToken}")
                  .check(status().is(200))
                  .check(jsonPath("$.phoneNumber").exists())
                  .check(jsonPath("$.authenticatedAt").exists()))
          .pause(1, 3)
          .exec(
              http("Get Chat Rooms")
                  .get("/api/v1/chat/rooms")
                  .header("Cookie", "LINKWAVE_SESSION=#{sessionToken}")
                  .check(status().is(200)))
          .pause(1, 3)
          .exec(
              http("Logout")
                  .post("/api/v1/auth/logout")
                  .header("Cookie", "LINKWAVE_SESSION=#{sessionToken}")
                  .check(status().in(200, 204)));

  private static final ScenarioBuilder OTP_REQUEST_SCENARIO =
      scenario("OTP Request Load").exec(REQUEST_OTP_FLOW);

  private static final ScenarioBuilder OTP_VERIFICATION_SCENARIO =
      scenario("OTP Verification Load").exec(VERIFY_OTP_FLOW);

  private static final ScenarioBuilder AUTHENTICATED_REQUEST_SCENARIO =
      scenario("Authenticated Request Load").exec(AUTHENTICATED_REQUEST_FLOW);

  {
    setUp(
            OTP_REQUEST_SCENARIO.injectOpen(rampUsers(100).during(Duration.ofSeconds(60))),
            OTP_VERIFICATION_SCENARIO.injectOpen(rampUsers(100).during(Duration.ofSeconds(60))),
            AUTHENTICATED_REQUEST_SCENARIO.injectOpen(
                rampUsers(100).during(Duration.ofSeconds(60))))
        .protocols(HTTP_PROTOCOL)
        .assertions(
            global().successfulRequests().percent().gte(95.0),
            global().responseTime().max().lt(5000),
            global().responseTime().percentile3().lt(2000),
            details("Request OTP").responseTime().percentile3().lt(1000),
            details("Verify OTP").responseTime().percentile3().lt(1500));
  }
}
