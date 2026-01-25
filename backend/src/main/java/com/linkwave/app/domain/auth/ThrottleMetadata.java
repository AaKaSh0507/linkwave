package com.linkwave.app.domain.auth;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Metadata for tracking OTP request throttling per phone number.
 */
public class ThrottleMetadata {
    
    private final List<Instant> requestTimestamps;

    public ThrottleMetadata() {
        this.requestTimestamps = new ArrayList<>();
    }

    public void addRequest(Instant timestamp) {
        requestTimestamps.add(timestamp);
    }

    public List<Instant> getRequestTimestamps() {
        return new ArrayList<>(requestTimestamps);
    }

    /**
     * Get count of requests within the specified time window.
     */
    public int getRequestCountWithinWindow(Instant windowStart) {
        return (int) requestTimestamps.stream()
            .filter(timestamp -> timestamp.isAfter(windowStart))
            .count();
    }

    /**
     * Clean up old timestamps outside the time window.
     */
    public void cleanupOldRequests(Instant windowStart) {
        requestTimestamps.removeIf(timestamp -> !timestamp.isAfter(windowStart));
    }
}
