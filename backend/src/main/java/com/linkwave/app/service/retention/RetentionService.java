package com.linkwave.app.service.retention;

import com.linkwave.app.config.retention.RetentionProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RetentionService {

  private static final Logger log = LoggerFactory.getLogger(RetentionService.class);

  private final RetentionBatchDeleter retentionBatchDeleter;
  private final RetentionProperties retentionProperties;
  private final Counter retentionJobSuccess;
  private final Counter retentionJobFailure;
  private final Counter retentionMessagesDeleted;
  private final Timer retentionJobTimer;

  public RetentionService(
      RetentionBatchDeleter retentionBatchDeleter,
      RetentionProperties retentionProperties,
      MeterRegistry meterRegistry) {
    this.retentionBatchDeleter = retentionBatchDeleter;
    this.retentionProperties = retentionProperties;
    this.retentionJobSuccess =
        Counter.builder("retention.job.executions.total")
            .tag("status", "success")
            .register(meterRegistry);
    this.retentionJobFailure =
        Counter.builder("retention.job.executions.total")
            .tag("status", "failure")
            .register(meterRegistry);
    this.retentionMessagesDeleted =
        Counter.builder("retention.messages.deleted.total").register(meterRegistry);
    this.retentionJobTimer = Timer.builder("retention.job.duration").register(meterRegistry);
  }

  public RetentionResult deleteExpiredMessages() {
    return retentionJobTimer.record(
        () -> {
          Instant cutoffTimestamp =
              Instant.now().minus(retentionProperties.getDays(), ChronoUnit.DAYS);
          long startTime = System.currentTimeMillis();
          long totalDeleted = 0;
          int batchCount = 0;

          log.info(
              "Starting message retention cleanup. cutoffTimestamp={}, retentionDays={},"
                  + " batchSize={}",
              cutoffTimestamp,
              retentionProperties.getDays(),
              retentionProperties.getBatchSize());

          try {
            int deletedInBatch;
            do {
              deletedInBatch = deleteBatchWithRetry(cutoffTimestamp);
              if (deletedInBatch > 0) {
                totalDeleted += deletedInBatch;
                batchCount++;
                log.info(
                    "Batch deletion completed. batchNumber={}, deletedInBatch={}, totalDeleted={},"
                        + " cutoffTimestamp={}",
                    batchCount,
                    deletedInBatch,
                    totalDeleted,
                    cutoffTimestamp);
              }
            } while (deletedInBatch > 0);

            long executionTimeMs = System.currentTimeMillis() - startTime;
            log.info(
                "Message retention cleanup completed. totalMessagesDeleted={}, totalBatches={},"
                    + " executionTimeMs={}",
                totalDeleted,
                batchCount,
                executionTimeMs);

            if (totalDeleted > 10000) {
              log.info(
                  "Large deletion detected. Consider running VACUUM ANALYZE on chat_messages table"
                      + " for optimal performance.");
            }

            retentionJobSuccess.increment();
            retentionMessagesDeleted.increment(totalDeleted);

            return new RetentionResult(totalDeleted, batchCount, executionTimeMs, true, null);

          } catch (Exception e) {
            long executionTimeMs = System.currentTimeMillis() - startTime;
            retentionJobFailure.increment();
            retentionMessagesDeleted.increment(totalDeleted);
            log.error(
                "Message retention cleanup failed. totalMessagesDeleted={}, totalBatches={},"
                    + " executionTimeMs={}, error={}",
                totalDeleted,
                batchCount,
                executionTimeMs,
                e.getMessage(),
                e);
            return new RetentionResult(
                totalDeleted, batchCount, executionTimeMs, false, e.getMessage());
          }
        });
  }

  private int deleteBatchWithRetry(Instant cutoffTimestamp) {
    int attempt = 0;
    long delay = retentionProperties.getRetryDelayMs();

    while (attempt < retentionProperties.getMaxRetries()) {
      try {
        long batchStartTime = System.currentTimeMillis();
        int deleted = retentionBatchDeleter.deleteBatch(cutoffTimestamp);
        long batchDuration = System.currentTimeMillis() - batchStartTime;

        log.debug(
            "Batch execution time. durationMs={}, messagesDeleted={}", batchDuration, deleted);

        if (batchDuration > 5000) {
          log.warn(
              "Slow batch deletion detected. durationMs={}, messagesDeleted={}",
              batchDuration,
              deleted);
        }

        return deleted;
      } catch (Exception e) {
        attempt++;
        if (attempt >= retentionProperties.getMaxRetries()) {
          log.error("Batch deletion failed after {} retries. error={}", attempt, e.getMessage(), e);
          throw e;
        }
        log.warn(
            "Batch deletion failed, retrying. attempt={}, maxRetries={}, error={}",
            attempt,
            retentionProperties.getMaxRetries(),
            e.getMessage());
        sleepWithExponentialBackoff(delay);
        delay *= 2;
      }
    }
    return 0;
  }

  private void sleepWithExponentialBackoff(long delay) {
    try {
      Thread.sleep(delay);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Retry interrupted", ie);
    }
  }

  public record RetentionResult(
      long totalMessagesDeleted,
      int batchesProcessed,
      long executionTimeMs,
      boolean successful,
      String errorMessage) {}
}
