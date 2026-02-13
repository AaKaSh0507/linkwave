package com.linkwave.app.domain.auth;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

  public int getRequestCountWithinWindow(Instant windowStart) {
    return (int)
        requestTimestamps.stream().filter(timestamp -> timestamp.isAfter(windowStart)).count();
  }

  public void cleanupOldRequests(Instant windowStart) {
    requestTimestamps.removeIf(timestamp -> !timestamp.isAfter(windowStart));
  }
}
