package com.linkwave.app;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.linkwave.app.service.retention.RetentionBatchDeleter;
import com.linkwave.app.service.retention.RetentionService;
import io.restassured.response.Response;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@TestPropertySource(
    properties = {
      "linkwave.retention.days=1",
      "linkwave.retention.batch-size=1000",
      "linkwave.retention.cron=*/10 * * * * *",
      "linkwave.retention.max-retries=3",
      "linkwave.retention.retry-delay-ms=50"
    })
@Tag("integration")
@Tag("slow")
class RetentionFunctionalTest extends FunctionalTestBase {

  @Autowired private RetentionService retentionService;

  @MockitoSpyBean private RetentionBatchDeleter retentionBatchDeleter;

  @BeforeEach
  void resetSpies() {
    reset(retentionBatchDeleter);
  }

  @Test
  @DisplayName("Retention job deletes old messages and keeps recent messages")
  void testRetentionJobDeletesOldMessages() {
    String roomId = createRoom();

    insertMessages(roomId, 10, Instant.now().minus(2, ChronoUnit.DAYS), "old");
    insertMessages(roomId, 10, Instant.now(), "recent");

    RetentionService.RetentionResult result = retentionService.deleteExpiredMessages();

    assertTrue(result.successful());
    assertTrue(countOldMessages() == 0, "Old messages should be deleted");
    assertTrue(countRecentMessages() == 10, "Recent messages should remain");
  }

  @Test
  @DisplayName("Retention job performs batch deletion and removes all old messages")
  void testRetentionJobBatchDeletion() {
    String roomId = createRoom();

    insertMessages(roomId, 5000, Instant.now().minus(2, ChronoUnit.DAYS), "batch-old");

    RetentionService.RetentionResult result = retentionService.deleteExpiredMessages();

    assertTrue(result.successful());
    assertTrue(countOldMessages() == 0, "All 5000 old messages should be deleted");
    assertTrue(result.batchesProcessed() >= 5, "Deletion should happen in batches");
    verify(retentionBatchDeleter, atLeast(5)).deleteBatch(any());
  }

  @Test
  @DisplayName("Retention scheduler runs automatically and deletes expired messages")
  void testRetentionJobScheduledExecution() {
    String roomId = createRoom();
    insertMessages(roomId, 20, Instant.now().minus(2, ChronoUnit.DAYS), "scheduled-old");

    await().atMost(15, TimeUnit.SECONDS).until(() -> countOldMessages() == 0);

    assertTrue(countOldMessages() == 0, "Scheduled retention should delete old messages");
  }

  @Test
  @DisplayName("Retention job handles deletion errors gracefully with retries")
  void testRetentionJobHandlesErrors() {
    doThrow(new RuntimeException("Simulated DB failure"))
        .when(retentionBatchDeleter)
        .deleteBatch(any());

    RetentionService.RetentionResult result = retentionService.deleteExpiredMessages();

    assertFalse(result.successful(), "Retention should report unsuccessful result on failure");
    assertNotNull(result.errorMessage(), "Error message should be populated");
    verify(retentionBatchDeleter, times(3)).deleteBatch(any());
  }

  @Test
  @DisplayName("Retention execution updates Prometheus retention metrics")
  void testRetentionJobUpdatesMetrics() {
    String roomId = createRoom();
    insertMessages(roomId, 15, Instant.now().minus(2, ChronoUnit.DAYS), "metrics-old");

    RetentionService.RetentionResult result = retentionService.deleteExpiredMessages();
    assertTrue(result.successful());

    Response metrics =
        given().when().get("/actuator/prometheus").then().statusCode(200).extract().response();

    String body = metrics.getBody().asString();
    assertTrue(body.contains("retention_messages_deleted_total"));
    assertTrue(body.contains("retention_job_duration_seconds"));
    assertTrue(body.contains("retention_job_executions_total"));
  }

  private String createRoom() {
    String roomId = UUID.randomUUID().toString();
    Instant now = Instant.now();

    jdbcTemplate.update(
        "INSERT INTO chat_rooms (id, room_type, name, created_at, updated_at) VALUES (?, ?, ?, ?,"
            + " ?)",
        roomId,
        "DIRECT",
        null,
        Timestamp.from(now),
        Timestamp.from(now));

    return roomId;
  }

  private void insertMessages(String roomId, int count, Instant sentAt, String prefix) {
    List<Object[]> batch = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      batch.add(
          new Object[] {
            UUID.randomUUID().toString(),
            roomId,
            "+1415557" + String.format("%04d", i),
            prefix + "-message-" + i,
            Timestamp.from(sentAt)
          });
    }

    jdbcTemplate.batchUpdate(
        "INSERT INTO chat_messages (id, room_id, sender_phone, body, sent_at) VALUES (?, ?, ?, ?,"
            + " ?)",
        batch);
  }

  private long countOldMessages() {
    Long count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM chat_messages WHERE sent_at < ?",
            Long.class,
            Timestamp.from(Instant.now().minus(1, ChronoUnit.DAYS)));
    return count == null ? 0L : count;
  }

  private long countRecentMessages() {
    Long count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM chat_messages WHERE sent_at >= ?",
            Long.class,
            Timestamp.from(Instant.now().minus(1, ChronoUnit.DAYS)));
    return count == null ? 0L : count;
  }
}
