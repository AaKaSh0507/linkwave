package com.linkwave.app.service.retention;

import com.linkwave.app.config.retention.RetentionProperties;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(RetentionScheduler.class);

    private final RetentionService retentionService;
    private final RetentionProperties retentionProperties;

    public RetentionScheduler(RetentionService retentionService, RetentionProperties retentionProperties) {
        this.retentionService = retentionService;
        this.retentionProperties = retentionProperties;
    }

    // Runs every Sunday at 2:00 AM UTC
    @Scheduled(cron = "${linkwave.retention.cron:0 0 2 ? * SUN}", zone = "UTC")
    @SchedulerLock(
            name = "messageRetentionCleanup",
            lockAtMostFor = "PT2H",
            lockAtLeastFor = "PT5M"
    )
    public void executeRetentionCleanup() {
        log.info("Starting scheduled message retention cleanup. retentionDays={}", retentionProperties.getDays());

        try {
            RetentionService.RetentionResult result = retentionService.deleteExpiredMessages();

            if (result.successful()) {
                log.info("Scheduled retention cleanup completed successfully. totalMessagesDeleted={}, batchesProcessed={}, executionTimeMs={}",
                        result.totalMessagesDeleted(), result.batchesProcessed(), result.executionTimeMs());
            } else {
                log.error("Scheduled retention cleanup completed with errors. totalMessagesDeleted={}, batchesProcessed={}, executionTimeMs={}, error={}",
                        result.totalMessagesDeleted(), result.batchesProcessed(), result.executionTimeMs(), result.errorMessage());
            }
        } catch (Exception e) {
            log.error("Unexpected error during scheduled retention cleanup. error={}", e.getMessage(), e);
        }
    }
}

