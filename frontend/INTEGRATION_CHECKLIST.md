# Backend Integration Checklist

Use this checklist to ensure your Spring Boot backend is compatible with the frontend application.

## API Endpoints

### Authentication Endpoints

- [ ] **POST /api/v1/auth/request-otp**
  - **Request Body**: `{ "phoneNumber": "+1234567890" }`
  - **Response**: `{ "otpId": "uuid-string", "message": "OTP sent" }`
  - **HTTP Status**: 200
  - **Notes**: Should send OTP via email/SMS. Store otpId for verification.

- [ ] **POST /api/v1/auth/verify-otp**
  - **Request Body**: `{ "otpId": "uuid-string", "otp": "123456" }`
  - **Response**: `{ "token": "jwt-token", "user": { "id": "user-id", "phoneNumber": "+1234567890", "displayName": "John Doe" } }`
  - **HTTP Status**: 200
  - **Notes**: Return JWT token. Token should be valid for auth header in subsequent requests.

### Conversation Endpoints

- [ ] **GET /api/v1/conversations**
  - **Headers**: `Authorization: Bearer {token}`
  - **Response**: `[ { "id": "conv-1", "participantIds": ["user-2"], "lastMessage": { "id": "msg-1", "content": "Hi", "timestamp": "2024-01-15T10:30:00Z" }, "lastMessageTime": "2024-01-15T10:30:00Z", "unreadCount": 0 } ]`
  - **HTTP Status**: 200
  - **Notes**: Return conversations for authenticated user.

- [ ] **POST /api/v1/conversations**
  - **Headers**: `Authorization: Bearer {token}`
  - **Request Body**: `{ "participantIds": ["user-id-to-chat-with"] }`
  - **Response**: Same as individual conversation object
  - **HTTP Status**: 201 or 200
  - **Notes**: Create new conversation or return existing one.

- [ ] **GET /api/v1/conversations/{conversationId}/messages**
  - **Headers**: `Authorization: Bearer {token}`
  - **Query Params**: `?limit=50` (frontend sends MESSAGE_PAGE_SIZE)
  - **Response**: `[ { "id": "msg-1", "conversationId": "conv-1", "senderId": "user-1", "content": "Hello", "timestamp": "2024-01-15T10:30:00Z", "isRead": true, "readAt": "2024-01-15T10:31:00Z" } ]`
  - **HTTP Status**: 200
  - **Notes**: Return messages for conversation, support pagination with limit param.

- [ ] **POST /api/v1/conversations/{conversationId}/messages**
  - **Headers**: `Authorization: Bearer {token}`
  - **Request Body**: `{ "content": "Hello there!" }`
  - **Response**: Same as message object
  - **HTTP Status**: 201
  - **Notes**: Create and return new message. Also send via WebSocket to other participant.

### User Endpoints

- [ ] **GET /api/v1/users/search**
  - **Headers**: `Authorization: Bearer {token}`
  - **Query Params**: `?q=phone-number`
  - **Response**: `[ { "id": "user-2", "phoneNumber": "+1234567890", "displayName": "Jane Doe", "status": "online", "lastSeen": "2024-01-15T10:30:00Z" } ]`
  - **HTTP Status**: 200
  - **Notes**: Search users by phone number. Return basic user info without sensitive data.

## WebSocket Integration

The frontend expects WebSocket connection at: `ws://localhost:8080/chat?token={jwt-token}`

### WebSocket Event Format

All WebSocket messages should follow this structure:
```json
{
  "type": "MESSAGE|TYPING|PRESENCE|READ_RECEIPT",
  "payload": { "conversationId": "...", ... }
}
```

### Server → Client Events

- [ ] **MESSAGE Event**
  - **Event Type**: `MESSAGE`
  - **Payload**: `{ "id": "msg-1", "conversationId": "conv-1", "senderId": "user-1", "content": "Hello", "timestamp": "2024-01-15T10:30:00Z", "isRead": false }`
  - **When**: When new message arrives in conversation
  - **Frontend Expects**: Message to appear in thread immediately

- [ ] **TYPING Event** (if feature enabled)
  - **Event Type**: `TYPING`
  - **Payload**: `{ "conversationId": "conv-1", "userId": "user-2", "isTyping": true }`
  - **When**: When user starts/stops typing
  - **Frontend Expects**: Typing indicator to show/hide

- [ ] **PRESENCE Event** (if feature enabled)
  - **Event Type**: `PRESENCE`
  - **Payload**: `{ "userId": "user-2", "status": "online", "lastSeen": "2024-01-15T10:30:00Z" }`
  - **When**: When user comes online/offline
  - **Frontend Expects**: Status to update in conversation list

- [ ] **READ_RECEIPT Event** (if feature enabled)
  - **Event Type**: `READ_RECEIPT`
  - **Payload**: `{ "messageId": "msg-1", "userId": "user-1", "readAt": "2024-01-15T10:31:00Z" }`
  - **When**: When message is read
  - **Frontend Expects**: Message read status to update

### Client → Server Events

- [ ] **SEND_MESSAGE Event**
  - **Event Type**: `SEND_MESSAGE`
  - **Payload**: `{ "conversationId": "conv-1", "content": "Hello there!" }`
  - **Backend Should**: Save message, send MESSAGE event to other participant

- [ ] **TYPING Event**
  - **Event Type**: `TYPING`
  - **Payload**: `{ "conversationId": "conv-1", "isTyping": true }`
  - **Backend Should**: Broadcast TYPING event to other participants in conversation

- [ ] **READ_RECEIPT Event**
  - **Event Type**: `READ_RECEIPT`
  - **Payload**: `{ "messageId": "msg-1" }`
  - **Backend Should**: Mark message as read, send READ_RECEIPT event to sender

## Security & Headers

- [ ] **CORS Headers**
  - Should allow requests from frontend origin
  - Example: `Access-Control-Allow-Origin: http://localhost:3000`
  - Should allow `Authorization` header

- [ ] **JWT Token Validation**
  - Validate token from `Authorization: Bearer {token}` header
  - Token should contain `sub` (subject/user ID)
  - Should reject expired tokens with 401 status
  - Should reject invalid tokens with 401 status

- [ ] **WebSocket Auth**
  - Validate token from query parameter `?token={jwt-token}`
  - Disconnect if token is invalid or expired
  - Store authenticated user ID for message routing

## Data Format Requirements

### User Object
```typescript
{
  id: string              // Unique user ID
  phoneNumber: string     // Phone number with country code
  displayName?: string    // Optional display name
  profilePicture?: string // Optional profile picture URL
  status: "online" | "offline"  // Current status
  lastSeen?: Date         // Last activity timestamp
}
```

### Message Object
```typescript
{
  id: string              // Unique message ID
  conversationId: string  // Conversation ID
  senderId: string        // Sender user ID
  content: string         // Message content (max 5000 chars)
  timestamp: Date         // When message was sent
  isRead: boolean         // Whether recipient read it
  readAt?: Date           // When message was read
}
```

### Conversation Object
```typescript
{
  id: string              // Unique conversation ID
  participantIds: string[] // Array of participant user IDs
  lastMessage?: Message   // Last message in conversation
  lastMessageTime?: Date  // When last message was sent
  unreadCount: number     // Number of unread messages
}
```

## Testing the Integration

### 1. Test Authentication
```bash
# Request OTP
curl -X POST http://localhost:8080/api/v1/auth/request-otp \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber": "+1234567890"}'

# Should get response with otpId
```

### 2. Test Conversations
```bash
# Get conversations (replace TOKEN with actual JWT)
curl -X GET http://localhost:8080/api/v1/conversations \
  -H "Authorization: Bearer TOKEN"

# Create conversation
curl -X POST http://localhost:8080/api/v1/conversations \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"participantIds": ["user-id-here"]}'
```

### 3. Test WebSocket
```bash
# Use wscat or similar tool
npm install -g wscat

# Connect to WebSocket
wscat -c "ws://localhost:8080/chat?token=JWT_TOKEN"

# Send message event
{"type": "SEND_MESSAGE", "payload": {"conversationId": "conv-id", "content": "Test"}}
```

## Common Issues & Fixes

### "401 Unauthorized" on API calls
- Verify JWT token is valid and not expired
- Check Authorization header format: `Bearer {token}`
- Ensure token validation logic is correct

### "405 Method Not Allowed"
- Verify correct HTTP method (GET, POST, etc.)
- Check endpoint URL matches expected format
- Ensure endpoint is implemented in backend

### WebSocket connection fails
- Check WebSocket endpoint path (should be `/chat`)
- Verify token is passed in query params: `?token={jwt}`
- Check CORS/proxy settings if behind reverse proxy
- Ensure WebSocket upgrade headers are correct

### Messages not appearing in real-time
- Verify MESSAGE event is sent after saving to database
- Check WebSocket connection is active (console logs)
- Ensure correct conversationId in event payload
- Verify other participant is connected to WebSocket

## Performance Considerations

- [ ] **Message Pagination**: Implement limit parameter for `/messages` endpoint
- [ ] **Conversation Sorting**: Sort by lastMessageTime descending (most recent first)
- [ ] **Database Indexes**: Index on (conversationId, timestamp) for messages
- [ ] **Message Retention**: Implement auto-delete for messages older than retention days
- [ ] **Connection Pool**: Use appropriate connection pool size for WebSocket

## Feature Flags Alignment

Ensure backend respects frontend feature flags:

- **If `NEXT_PUBLIC_TYPING_INDICATORS=false`**: Don't send TYPING events
- **If `NEXT_PUBLIC_READ_RECEIPTS=false`**: Don't send READ_RECEIPT events
- **If `NEXT_PUBLIC_PRESENCE=false`**: Don't send PRESENCE events

Or alternatively, frontend simply won't display these features even if backend sends them.

## Deployment Checklist

- [ ] Verify all API URLs use HTTPS in production
- [ ] Verify WebSocket URLs use WSS (secure WebSocket) in production
- [ ] Ensure CORS is configured for production domain
- [ ] Test all endpoints with actual frontend
- [ ] Monitor WebSocket connection stability
- [ ] Implement rate limiting for auth endpoints
- [ ] Set appropriate CORS headers for security
- [ ] Validate and sanitize all user input
- [ ] Use prepared statements to prevent SQL injection
- [ ] Implement proper error handling and logging

This checklist ensures smooth integration between frontend and backend applications.
