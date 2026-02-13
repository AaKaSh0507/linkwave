package com.linkwave.app.util;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.Callable;

public final class AsyncAssertions {

  private AsyncAssertions() {}

  public static void assertEventually(Callable<Boolean> condition, long timeoutSeconds) {
    await().atMost(Duration.ofSeconds(timeoutSeconds)).until(condition);
  }

  public static void assertWithinMillis(Runnable action, long maxMillis) {
    long start = System.currentTimeMillis();
    action.run();
    long duration = System.currentTimeMillis() - start;
    if (duration > maxMillis) {
      throw new AssertionError(
          "Action exceeded max duration. maxMillis=" + maxMillis + ", actualMillis=" + duration);
    }
  }
}
