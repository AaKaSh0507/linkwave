package com.linkwave.app.domain.chat;

import java.time.Instant;
import java.util.List;

public class PaginatedMessagesResponse {
  private List<MessageDTO> messages;
  private boolean hasMore;
  private Instant oldestTimestamp;

  public PaginatedMessagesResponse() {}

  public PaginatedMessagesResponse(
      List<MessageDTO> messages, boolean hasMore, Instant oldestTimestamp) {
    this.messages = messages;
    this.hasMore = hasMore;
    this.oldestTimestamp = oldestTimestamp;
  }

  public static PaginatedMessagesResponse empty() {
    return new PaginatedMessagesResponse(List.of(), false, null);
  }

  public static PaginatedMessagesResponse of(List<MessageDTO> messages, boolean hasMore) {
    Instant oldest = messages.isEmpty() ? null : messages.get(messages.size() - 1).getSentAt();
    return new PaginatedMessagesResponse(messages, hasMore, oldest);
  }

  public List<MessageDTO> getMessages() {
    return messages;
  }

  public void setMessages(List<MessageDTO> messages) {
    this.messages = messages;
  }

  public boolean isHasMore() {
    return hasMore;
  }

  public void setHasMore(boolean hasMore) {
    this.hasMore = hasMore;
  }

  public Instant getOldestTimestamp() {
    return oldestTimestamp;
  }

  public void setOldestTimestamp(Instant oldestTimestamp) {
    this.oldestTimestamp = oldestTimestamp;
  }
}
