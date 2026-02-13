package com.linkwave.app.controller.presence;

import com.linkwave.app.service.presence.PresenceService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/presence")
public class PresenceController {

  private static final Logger log = LoggerFactory.getLogger(PresenceController.class);

  private final PresenceService presenceService;

  public PresenceController(PresenceService presenceService) {
    this.presenceService = presenceService;
  }

  @GetMapping("/{userId}")
  public ResponseEntity<PresenceResponse> getUserPresence(@PathVariable String userId) {
    log.debug("Checking presence for user: {}", maskUserId(userId));

    boolean online = presenceService.isUserOnline(userId);
    Instant lastSeen = presenceService.getLastSeen(userId);

    PresenceResponse response = new PresenceResponse(userId, online, lastSeen);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/bulk")
  public ResponseEntity<BulkPresenceResponse> getBulkPresence(
      @RequestBody BulkPresenceRequest request) {
    log.debug("Checking presence for {} users", request.userIds().size());

    Map<String, Boolean> presenceMap = presenceService.getUsersPresence(request.userIds());

    BulkPresenceResponse response = new BulkPresenceResponse(presenceMap);
    return ResponseEntity.ok(response);
  }

  private String maskUserId(String userId) {
    if (userId == null || userId.length() < 7) {
      return "***";
    }
    return userId.substring(0, 4) + "***" + userId.substring(userId.length() - 2);
  }

  public record PresenceResponse(String userId, boolean online, Instant lastSeen) {}

  public record BulkPresenceRequest(List<String> userIds) {}

  public record BulkPresenceResponse(Map<String, Boolean> presence) {}
}
