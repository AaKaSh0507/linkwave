# Typing Indicators Design

## Overview

Typing indicators provide realtime feedback to users when other participants in a chat room are actively typing. This feature is implemented as an ephemeral, in-memory system with automatic cleanup and rate limiting.

## Architecture

### Components

1. **TypingEvent** (`domain/typing/TypingEvent.java`)
   - JSON-serializable event model
   - Actions: `START`, `STOP`
   - Fields: `type`, `action`, `senderId`, `roomId`, `timestamp`

2. **TypingStateManager** (`service/typing/TypingStateManager.java`)
   - In-memory state tracking using `ConcurrentHashMap`
   - Thread-safe for concurrent WebSocket connections
   - Manages typing state per room
   - Implements rate limiting and auto-timeout

3. **TypingCleanupBroadcaster** (`service/typing/TypingCleanupBroadcaster.java`)
   - Scheduled task running every 2 seconds
   - Broadcasts `typing.stop` for expired typing states
   - Ensures stale indicators are automatically removed

4. **NativeWebSocketHandler** (`websocket/NativeWebSocketHandler.java`)
   - Handles `typing.start` and `typing.stop` WebSocket messages
   - Validates room membership before processing
   - Broadcasts typing events to room members (excluding sender)
   - Clears typing state on disconnect

## Message Flow

### Client → Server

**typing.start**
```json
{
  "type": "typing.start",
  "roomId": "room-123"
}
```

**typing.stop**
```json
{
  "type": "typing.stop",
  "roomId": "room-123"
}
```

### Server → Client

**Broadcast to room members**
```json
{
  "type": "typing.event",
  "action": "start",
  "senderId": "+14155551234",
  "roomId": "room-123",
  "timestamp": 1706634000000
}
```

## Typing Lifecycle

### 1. User Starts Typing

1. Client sends `typing.start` message with `roomId`
2. Server validates user is a member of the room
3. Server checks rate limit (minimum 2 seconds between starts)
4. If accepted:
   - State manager records typing state with current timestamp
   - Server broadcasts `typing.event` with `action: "start"` to all room members except sender
5. If rate-limited:
   - Request is silently ignored (no broadcast)

### 2. User Stops Typing

1. Client sends `typing.stop` message with `roomId`
2. Server removes typing state for that user/session/room
3. Server broadcasts `typing.event` with `action: "stop"` to all room members except sender

### 3. Auto-Timeout (No Activity)

1. Scheduled task runs every 2 seconds
2. Identifies typing states older than 5 seconds
3. Removes stale states
4. Broadcasts `typing.stop` for each expired state

### 4. User Disconnects

1. WebSocket connection closes
2. `afterConnectionClosed` handler calls `typingStateManager.clearUserTyping()`
3. Returns list of affected rooms
4. Server broadcasts `typing.stop` for each affected room

## Technical Specifications

### Timeout Value
- **5 seconds** - Configured in `TypingStateManager.TYPING_TIMEOUT_SECONDS`
- Cleanup runs every 2 seconds via `@Scheduled(fixedDelay = 2000)`
- Rationale: Balance between responsiveness and avoiding flicker

### Rate Limiting
- **2 seconds** minimum between `typing.start` events per user per room
- Configured in `TypingStateManager.RATE_LIMIT_SECONDS`
- Duplicate `typing.start` within window returns `false` (not broadcast)
- Rationale: Prevent spam while allowing natural typing patterns

### State Storage
- **In-memory only** - `ConcurrentHashMap<String, Set<TypingState>>`
- Key: `roomId`
- Value: Set of `TypingState` objects (userId, sessionId, lastActivity timestamp)
- Thread-safe for concurrent WebSocket connections
- **No persistence** to Redis, Postgres, or Kafka

### Broadcast Strategy
- **Direct WebSocket** - Via `NativeWebSocketHandler.sendToUser()`
- Room-scoped - Only to room members
- Excludes sender - Sender never receives own typing events
- **No Kafka** - Typing events are ephemeral, not persisted

### Multi-Device Behavior
- Typing is tracked per connection (session), not per user globally
- Each device/session is independent
- If user types on two devices:
  - Both sessions are tracked separately
  - Both show as typing independently
- If all connections stop typing or disconnect:
  - Typing indicator disappears

## Room Isolation

Typing indicators are strictly isolated to rooms:
- User must be a member of the room to send typing events
- Typing events are only broadcast to members of that specific room
- Typing in Room X is never visible in Room Y

## Error Handling

### Invalid Requests
- Missing `roomId`: Logged as warning, silently ignored
- User not in room: Logged as warning, silently ignored
- Rate-limited: Logged as debug, silently ignored

### Broadcast Failures
- Errors during broadcast are caught and logged
- Does not affect other room members
- Does not crash the WebSocket handler

## Performance Considerations

### Memory Usage
- Minimal: Only stores active typing states
- Automatic cleanup removes stale states
- No unbounded growth

### Network Traffic
- Low: Only broadcasts when state changes
- Rate limiting prevents spam
- No polling required

### CPU Usage
- Minimal: Scheduled cleanup runs every 2 seconds
- O(n) where n = number of active typing states
- Typically very small (< 100 states)

## Testing

### Unit Tests
- `TypingStateManagerTest.java` - State management logic
  - Add/remove typers
  - Auto-timeout behavior
  - Concurrent access
  - Rate limiting

### Integration Tests
- `NativeWebSocketHandlerTypingTest.java` - WebSocket integration
  - `typing.start` broadcast
  - `typing.stop` broadcast
  - Room membership validation
  - Rate limiting integration
  - Disconnect cleanup
  - Sender exclusion

## Monitoring

### Metrics Available
- `TypingStateManager.getStats()` returns:
  - `activeRooms`: Number of rooms with active typers
  - `typingUsers`: Total number of active typing states

### Logging
- Debug level: Typing start/stop, rate limiting
- Info level: None (typing is ephemeral)
- Warn level: Invalid requests (missing roomId, not in room)
- Error level: Broadcast failures

## Future Enhancements

Potential improvements (not currently implemented):
1. **Typing user names**: Include sender name in broadcast for UI display
2. **Multiple typers display**: "Alice and Bob are typing..."
3. **Metrics dashboard**: Real-time typing activity monitoring
4. **Configurable timeouts**: Per-room or per-user timeout values
5. **Typing analytics**: Track typing patterns for UX insights

## Summary

The typing indicators system provides a robust, ephemeral, realtime feedback mechanism with:
- ✅ 5-second auto-timeout
- ✅ 2-second rate limiting
- ✅ In-memory storage (no persistence)
- ✅ Direct WebSocket broadcast (no Kafka)
- ✅ Room isolation and membership validation
- ✅ Multi-device support
- ✅ Automatic disconnect cleanup
- ✅ Comprehensive test coverage
