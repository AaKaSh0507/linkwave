package com.linkwave.app.controller.chat;

import com.linkwave.app.domain.auth.AuthenticatedUserContext;
import com.linkwave.app.domain.chat.PaginatedMessagesResponse;
import com.linkwave.app.service.chat.MessageHistoryService;
import com.linkwave.app.service.session.SessionService;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
public class MessageHistoryController {

  private final MessageHistoryService messageHistoryService;
  private final SessionService sessionService;

  public MessageHistoryController(
      MessageHistoryService messageHistoryService, SessionService sessionService) {
    this.messageHistoryService = messageHistoryService;
    this.sessionService = sessionService;
  }

  @GetMapping("/{recipientId}")
  public ResponseEntity<?> getMessageHistory(
      @PathVariable String recipientId, @RequestParam(required = false) String before) {

    AuthenticatedUserContext user = sessionService.getAuthenticatedUser().orElse(null);

    if (user == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
    }

    try {
      Instant beforeTimestamp = parseBeforeTimestamp(before);

      PaginatedMessagesResponse response =
          messageHistoryService.getDirectMessageHistoryPaginated(
              user.getPhoneNumber(), recipientId, beforeTimestamp);

      return ResponseEntity.ok(response);
    } catch (DateTimeParseException e) {
      return ResponseEntity.badRequest()
          .body("Invalid timestamp format. Use ISO-8601 UTC format (e.g., 2024-01-15T10:30:00Z)");
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Database error");
    }
  }

  private Instant parseBeforeTimestamp(String before) {
    if (before == null || before.isBlank()) {
      return null;
    }
    Instant timestamp = Instant.parse(before);
    if (timestamp.isAfter(Instant.now())) {
      return Instant.now();
    }
    return timestamp;
  }
}
