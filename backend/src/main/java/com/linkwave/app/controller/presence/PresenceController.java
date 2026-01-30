package com.linkwave.app.controller.presence;

import com.linkwave.app.service.presence.PresenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * REST controller for querying user presence status.
 * 
 * Phase D1: Presence Tracking
 * 
 * Endpoints:
 * - GET /api/v1/presence/{userId} - Check single user presence
 * - POST /api/v1/presence/bulk - Check multiple users presence
 * 
 * Authentication: Requires authenticated session
 * Authorization: Users can query presence of any user (future: restrict to
 * contacts)
 */
@RestController
@RequestMapping("/api/v1/presence")
public class PresenceController {

    private static final Logger log = LoggerFactory.getLogger(PresenceController.class);

    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    /**
     * Check if a single user is online.
     * 
     * @param userId User ID (phone number) to check
     * @return Presence status with online flag and last seen timestamp
     */
    @GetMapping("/{userId}")
    public ResponseEntity<PresenceResponse> getUserPresence(@PathVariable String userId) {
        log.debug("Checking presence for user: {}", maskUserId(userId));

        boolean online = presenceService.isUserOnline(userId);
        Instant lastSeen = presenceService.getLastSeen(userId);

        PresenceResponse response = new PresenceResponse(userId, online, lastSeen);
        return ResponseEntity.ok(response);
    }

    /**
     * Check presence for multiple users (bulk query).
     * Useful for contact lists.
     * 
     * @param request List of user IDs to check
     * @return Map of userId -> online status
     */
    @PostMapping("/bulk")
    public ResponseEntity<BulkPresenceResponse> getBulkPresence(@RequestBody BulkPresenceRequest request) {
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

    // DTOs

    public record PresenceResponse(
            String userId,
            boolean online,
            Instant lastSeen) {
    }

    public record BulkPresenceRequest(
            List<String> userIds) {
    }

    public record BulkPresenceResponse(
            Map<String, Boolean> presence) {
    }
}
