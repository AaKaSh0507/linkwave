# Read Receipts Design

## Overview

Read receipts provide persistent tracking of when users have read messages in chat rooms. This feature supports both 1-1 and group chats with correct, idempotent, and efficient message read tracking.

## Architecture

### Components

1. **ReadReceiptEntity** (`domain/chat/ReadReceiptEntity.java`)
   - JPA entity mapping to `read_receipts` table
   - Fields: `id`, `messageId`, `roomId`, `readerPhoneNumber`, `readAt`, `createdAt`
   - Unique constraint: `(messageId, readerPhoneNumber)` - ensures idempotency

2. **ReadReceiptEvent** (`domain/chat/ReadReceiptEvent.java`)
   - WebSocket broadcast event model
   - Fields: `type`, `roomId`, `messageId`, `readerId`, `timestamp`
   - JSON serialization with Jackson annotations

3. **ReadReceiptRepository** (`repository/ReadReceiptRepository.java`)
   - JPA repository for read receipt persistence
   - Custom queries for batch operations and read status
   - Idempotency checks and read history queries

4. **ReadReceiptService** (`service/readreceipt/ReadReceiptService.java`)
   - Business logic for marking messages as read
   - Batch read operations (up to 50 messages)
   - Room membership validation
   - Returns `ReadReceiptResult` indicating new vs duplicate reads

5. **NativeWebSocketHandler** (`websocket/NativeWebSocketHandler.java`)
   - Handles `read.up_to` WebSocket messages
   - Validates room membership
   - Broadcasts `read.receipt` events to room members (excluding reader)
   - Error handling for validation failures

## Database Schema

### read_receipts Table

```sql
CREATE TABLE read_receipts (
    id BIGSERIAL PRIMARY KEY,
    message_id VARCHAR(36) NOT NULL,
    room_id VARCHAR(36) NOT NULL,
    reader_phone_number VARCHAR(20) NOT NULL,
    read_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT uq_read_receipt UNIQUE (message_id, reader_phone_number),
    CONSTRAINT fk_read_receipt_room FOREIGN KEY (room_id) 
        REFERENCES chat_rooms(id) ON DELETE CASCADE
);

CREATE INDEX idx_read_receipts_message ON read_receipts(message_id);
CREATE INDEX idx_read_receipts_room_reader ON read_receipts(room_id, reader_phone_number);
CREATE INDEX idx_read_receipts_room ON read_receipts(room_id);
```

**Key Features:**
- **Unique constraint**: Prevents duplicate reads at database level
- **Foreign key**: Cascade delete when room is deleted
- **Indexes**: Optimized for common queries (by message, by room+reader, by room)

## Message Flow

### Client → Server (read.up_to)

```json
{
  "type": "read.up_to",
  "roomId": "room-123",
  "messageId": "msg-456"
}
```

**Semantics**: "I have read all messages up to and including msg-456 in room-123"

**Processing:**
1. Validate `roomId` and `messageId` are present
2. Call `ReadReceiptService.markReadUpTo(roomId, messageId, userId)`
3. Service finds all unread messages up to target timestamp (max 50)
4. For each unread message:
   - Check if already read (idempotency)
   - Validate room membership
   - Persist read receipt
5. Return list of new reads
6. Broadcast each new read receipt to room members

### Server → Client (read.receipt)

```json
{
  "type": "read.receipt",
  "roomId": "room-123",
  "messageId": "msg-456",
  "readerId": "+14155551234",
  "timestamp": 1706634000000
}
```

**Broadcast**: Sent to all room members except the reader

## Read Receipt Lifecycle

### 1. User Reads Messages

1. Client sends `read.up_to` with latest visible message ID
2. Server validates user is authenticated and in room
3. Server queries unread messages up to target timestamp
4. Server persists read receipts (idempotent)
5. Server broadcasts new read receipts to room members

### 2. Idempotency Handling

**Database Level:**
- Unique constraint on `(message_id, reader_phone_number)`
- PostgreSQL prevents duplicate inserts
- Constraint violations are handled gracefully

**Application Level:**
- `existsByMessageIdAndReaderPhoneNumber()` check before insert
- Returns `ReadReceiptResult.alreadyRead()` if duplicate
- No broadcast for duplicate reads

**Retry Safety:**
- Client can safely retry `read.up_to` messages
- Duplicate reads are detected and ignored
- No performance penalty for retries

### 3. Batch Read Operation

**"Read up to X" Semantics:**
1. Find target message timestamp
2. Query all unread messages with `sentAt <= targetTimestamp`
3. Limit to 50 messages to avoid long transactions
4. Mark each as read (with idempotency check)
5. Broadcast each new read

**Optimization:**
- Check `findMaxReadMessageTimestamp()` first
- Skip if user has already read newer messages
- Avoids re-processing old messages

### 4. Broadcast to Room Members

1. Query room members via `RoomMembershipService`
2. Create `ReadReceiptEvent` with read details
3. Serialize to JSON
4. Send to each member's active WebSocket session
5. **Exclude the reader** from broadcast
6. Handle errors gracefully (log and continue)

## Data Model Choice

### Decision: Per-Message Read Tracking

**Chosen Approach**: `read_receipts` table with one row per `(message_id, reader_phone_number)`

**Advantages:**
1. **Group chat support**: Can track multiple readers per message
2. **Query flexibility**: Can answer "who read message X?" and "what did user Y read?"
3. **Idempotency**: Unique constraint prevents duplicates
4. **Audit trail**: Preserves `read_at` timestamp for each read
5. **Simple queries**: Standard SQL joins and counts

**Alternative Considered**: Per-room "last_read_message_id" per user

**Why Rejected:**
- Cannot track multiple readers in group chats
- Cannot show "Seen by Alice, Bob" in UI
- Loses granular read history
- Harder to implement "read up to X" semantics
- No audit trail of when messages were read

## Group Chat Handling

### Tracking Multiple Readers

Each user's read creates a separate row:

```sql
SELECT reader_phone_number, read_at 
FROM read_receipts 
WHERE message_id = 'msg-456'
ORDER BY read_at ASC;
```

**Result:**
```
+---------------------+-------------------------+
| reader_phone_number | read_at                 |
+---------------------+-------------------------+
| +14155551234        | 2026-01-30 10:15:30     |
| +14155555678        | 2026-01-30 10:16:45     |
| +14155559999        | 2026-01-30 10:17:12     |
+---------------------+-------------------------+
```

### Read Count

```sql
SELECT COUNT(*) FROM read_receipts WHERE message_id = 'msg-456';
```

Useful for "Seen by 3 people" UI.

### Read Status Per User

```sql
SELECT message_id 
FROM read_receipts 
WHERE room_id = 'room-123' 
  AND reader_phone_number = '+14155551234';
```

Determines which messages a specific user has read.

## Idempotency Enforcement

### Database Level
- **Unique constraint**: `(message_id, reader_phone_number)`
- **Prevents**: Duplicate rows even under concurrent writes
- **Behavior**: PostgreSQL returns constraint violation error
- **Handling**: Application catches and treats as already read

### Application Level
- **Check**: `existsByMessageIdAndReaderPhoneNumber()` before insert
- **Returns**: `ReadReceiptResult.alreadyRead()` if duplicate
- **Broadcast**: Only new reads are broadcast
- **Transaction**: Isolation ensures consistency

### Retry Safety
- Client can safely retry `read.up_to` messages
- Duplicate reads are detected at both levels
- No duplicate broadcasts
- No performance penalty

## Performance Considerations

### Batch Reads ("read up to X")

**Query Strategy:**
```sql
SELECT m.id FROM chat_messages m 
WHERE m.room_id = :roomId 
  AND m.sent_at <= :targetTimestamp 
  AND m.id NOT IN (
    SELECT r.message_id FROM read_receipts r 
    WHERE r.room_id = :roomId 
      AND r.reader_phone_number = :readerPhoneNumber
  ) 
ORDER BY m.sent_at ASC
LIMIT 50;
```

**Optimizations:**
1. **Limit to 50 messages**: Avoids long transactions
2. **Check max read timestamp**: Skip if already read newer
3. **Indexed queries**: Fast lookups via indexes
4. **Single transaction**: Atomic batch operation

### Indexes

- `idx_read_receipts_message` - Fast lookup by message_id
- `idx_read_receipts_room_reader` - Fast lookup for user's reads in a room
- `idx_read_receipts_room` - Fast room-wide queries

### Broadcast Efficiency

- **Direct WebSocket**: No Kafka overhead
- **Only new reads**: Skips duplicates
- **Room-scoped**: Only to members
- **Async**: Non-blocking broadcast

### Memory Usage

- **Minimal**: Only active WebSocket sessions in memory
- **No caching**: Database is source of truth
- **Stateless**: No in-memory read state

## Offline and Reconnect Handling

### Persistence is Source of Truth

- Read receipts are persisted in PostgreSQL
- Survive client refresh, reconnect, server restart
- No in-memory state to lose

### No Re-broadcast on Reconnect

- WebSocket handler only broadcasts new reads
- Old reads are already in database
- Frontend can query read status on load (future REST API)

### Idempotent Reads

- Client can re-send `read.up_to` after reconnect
- Duplicate reads are detected and not broadcast
- No visual flicker or duplicate notifications

## Error Handling

### Invalid Requests

- **Missing roomId**: Logged as warning, silently ignored
- **Missing messageId**: Logged as warning, silently ignored
- **User not in room**: `UnauthorizedException`, logged as warning
- **Message not found**: `NotFoundException`, logged as warning

### Broadcast Failures

- Errors during broadcast are caught and logged
- Does not affect other room members
- Does not crash the WebSocket handler

### Database Errors

- Constraint violations (duplicate reads) are handled gracefully
- Transaction rollback on errors
- Service returns appropriate result

## Testing Strategy

### Unit Tests - ReadReceiptServiceTest

✅ All tests passing:
- `markMessageRead()` - new read persistence
- `markMessageRead()` - duplicate read (idempotency)
- `markMessageRead()` - unauthorized user (not in room)
- `markReadUpTo()` - batch reads up to target message

### Integration Tests - NativeWebSocketHandlerReadReceiptTest

✅ All tests passing:
1. Valid message → persist and broadcast
2. Duplicate read → no duplicate broadcast
3. User not in room → rejected
4. Message not found → handled gracefully
5. Missing roomId → ignored
6. Missing messageId → ignored
7. Group chat → multiple readers tracked
8. Broadcast excludes reader

### Manual Verification Checklist

1. **1-1 Chat**:
   - User A sends message
   - User B opens chat
   - User B's client sends `read.up_to`
   - User A sees "Seen" indicator
   - Refresh page → read state preserved

2. **Group Chat**:
   - User A sends message in group
   - User B reads → "Seen by B"
   - User C reads → "Seen by B, C"
   - User A sees both read receipts

3. **Idempotency**:
   - User scrolls up and down
   - Same messages marked read multiple times
   - No duplicate "seen" notifications

4. **Persistence**:
   - User reads messages
   - Refreshes page
   - Read state preserved (from database)

5. **Offline**:
   - User reads messages
   - Disconnects
   - Reconnects
   - No duplicate read receipts broadcast

## Technical Specifications

| Aspect | Value | Rationale |
|--------|-------|-----------|
| **Data Model** | Per-message tracking | Group chat support, query flexibility |
| **Idempotency** | Unique constraint + app check | Database-level guarantee |
| **Batch Size** | 50 messages max | Balance between UX and transaction time |
| **Broadcast** | Direct WebSocket | Low latency, no Kafka overhead |
| **Storage** | PostgreSQL | Persistent, ACID guarantees |
| **Indexes** | 3 indexes | Optimized for common queries |

## API Summary

### ReadReceiptService

**markMessageRead(messageId, roomId, readerPhoneNumber)**
- Marks a single message as read
- Returns `ReadReceiptResult` (new or duplicate)
- Validates room membership
- Idempotent

**markReadUpTo(roomId, messageId, readerPhoneNumber)**
- Marks all messages up to target as read
- Batch operation (max 50 messages)
- Returns list of `ReadReceiptResult`
- Optimized with max read timestamp check

**getMessageReaders(messageId)**
- Returns list of phone numbers who read the message
- For group chat "Seen by X, Y" UI

**getReadCount(messageId)**
- Returns count of readers
- For "Seen by N people" UI

### WebSocket Events

**Client → Server**
- `read.up_to` - Mark messages as read

**Server → Client**
- `read.receipt` - Broadcast read notification

## Future Enhancements

Potential improvements (not currently implemented):

1. **REST API for read status**:
   - `GET /api/messages/{messageId}/readers` - Who read a message
   - `GET /api/rooms/{roomId}/read-status` - User's read status in room

2. **Read receipt settings**:
   - Per-user privacy settings (disable read receipts)
   - Per-room settings (disable in specific rooms)

3. **Read receipt analytics**:
   - Average time to read
   - Read rate per room
   - Engagement metrics

4. **Optimistic UI updates**:
   - Client immediately shows "read" before server confirms
   - Rollback on error

5. **Delivery receipts**:
   - Track when messages are delivered (separate from read)
   - "Delivered" vs "Read" status

## Summary

The read receipts system provides robust, persistent message read tracking with:

- ✅ Correct persistence with idempotency
- ✅ Efficient batch reads (up to 50 messages)
- ✅ Group chat support (multiple readers per message)
- ✅ Direct WebSocket broadcast (no Kafka)
- ✅ Offline/reconnect safety
- ✅ Room membership validation
- ✅ Comprehensive test coverage
- ✅ Production-ready implementation
