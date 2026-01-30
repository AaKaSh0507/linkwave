# Frontend Architecture

This document describes the structure, data flow, and design patterns of the chat application frontend.

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Browser / Client                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │         UI Components (React 19 + TypeScript)             │   │
│  │  - Auth Pages (Login, OTP Verification)                   │   │
│  │  - Chat Interface (Messages, Conversations)               │   │
│  │  - Contact Search Dialog                                  │   │
│  └──────────────────────────────────────────────────────────┘   │
│                              ↕                                    │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │       Context API + React Hooks State Management          │   │
│  │  - AuthContext (user, token, login/logout)                │   │
│  │  - ChatContext (conversations, messages, participants)    │   │
│  └──────────────────────────────────────────────────────────┘   │
│                              ↕                                    │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │            Network Layer (HTTP + WebSocket)               │   │
│  │  - API Client (REST for CRUD operations)                  │   │
│  │  - WebSocket Client (Real-time events)                    │   │
│  │  - Config Management (Environment variables)              │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              ↕ ↕
            ┌─────────────────────────────────────┐
            │    Spring Boot Backend Server        │
            │  - REST API (/api/v1/*)              │
            │  - WebSocket Server (/chat)          │
            │  - Database (PostgreSQL)             │
            └─────────────────────────────────────┘
```

## Component Hierarchy

```
RootLayout (AuthProvider, ChatProvider)
├── /auth/page.tsx (AuthPage)
│   ├── LoginForm
│   └── OTPVerification
│
└── /chat/page.tsx (ChatPage)
    ├── Sidebar
    │   ├── ConversationList
    │   └── ContactSearch
    │
    └── Main Content
        ├── MessageThread
        │   ├── Message[] (MessageBubble)
        │   ├── TypingIndicator
        │   └── MessageInput
        │
        └── ContactSearch Dialog
```

## State Management Flow

### Authentication Flow

```
1. User opens app
   └─→ RootLayout renders AuthProvider
       └─→ AuthContext initializes
           └─→ Check localStorage for auth token
               ├─ If token exists → set isAuthenticated = true
               └─ If no token → redirect to /auth

2. User enters phone on LoginForm
   └─→ requestOTP() called
       └─→ apiCall POST /auth/request-otp
           └─→ Backend returns otpId
               └─→ Store otpId in local state
                   └─→ Show OTPVerification form

3. User enters OTP on OTPVerification
   └─→ verifyOTP() called
       └─→ apiCall POST /auth/verify-otp
           └─→ Backend returns token + user
               └─→ Store token in localStorage
                   └─→ Store userId and phoneNumber
                       └─→ Set user in AuthContext
                           └─→ Redirect to /chat
```

### Chat Flow

```
1. User navigates to /chat
   └─→ ChatProvider initializes
       └─→ useWebSocket() hook connects to WebSocket
           ├─ Connection: ws://host:port/chat?token={token}
           └─ Register event listeners (MESSAGE, TYPING, PRESENCE, READ_RECEIPT)

2. fetchConversations() executes
   └─→ apiCall GET /conversations
       └─→ Backend returns conversation list
           └─→ Store in ChatContext.conversations
               └─→ Load participant info
                   └─→ Render ConversationList

3. User clicks conversation
   └─→ setCurrentConversation(conversation)
       └─→ fetchMessages(conversationId) called
           └─→ apiCall GET /conversations/{id}/messages?limit=50
               └─→ Store in ChatContext.messages
                   └─→ Render MessageThread

4. User types message
   └─→ MessageInput detects change
       └─→ sendTypingIndicator(conversationId, true)
           └─→ WebSocket sends TYPING event
               └─→ Backend broadcasts to other participant
                   └─→ Other user sees typing indicator

5. User sends message
   └─→ sendMessage(content) called
       └─→ apiCall POST /conversations/{id}/messages
           └─→ Optimistic update: addMessage(message)
               └─→ Backend confirms via WebSocket MESSAGE event
                   └─→ Update ChatContext.messages
                       └─→ Message appears in thread
```

## Real-Time Data Flow

### WebSocket Message Handling

```
User A connects        User B connects
    │                      │
    └─ ws://host/chat     └─ ws://host/chat
       (token=A)           (token=B)
       │                   │
       │ User A types      │
       │ "Hello"           │
       │                   │
       └─→ TYPING event ───→ Backend
                          │
           Backend broadcasts TYPING event to User B
                          │
                          ├─ Listener catches TYPING
                          │
                          └─→ TypingIndicator renders
                              (animated dots)
```

### Message Delivery Flow

```
User A sends "Hi"
    │
    └─→ sendMessage() ──→ REST API POST /messages
                         │
                         └─→ Backend saves to DB
                            │
                            ├─→ Send MESSAGE event via WebSocket to User B
                            │   Payload: { id, conversationId, content, timestamp, ... }
                            │
                            ├─→ User B receives MESSAGE event
                            │   │
                            │   └─→ addMessage() updates ChatContext.messages
                            │       │
                            │       └─→ MessageBubble re-renders
                            │           (message appears in UI)
                            │
                            └─→ User B automatically marks as read
                                │
                                └─→ Send READ_RECEIPT event back
                                    │
                                    └─→ User A receives READ_RECEIPT
                                        │
                                        └─→ Update message.isRead = true
                                            (show ✓✓ indicator)
```

## Data Persistence Strategy

### localStorage
- **Auth Token**: `auth_token` - JWT for API authentication
- **User ID**: `user_id` - Current user identifier
- **Phone**: `user_phone` - User's phone number

### Context (Memory)
- **Conversations**: List of all user's conversations
- **Current Conversation**: Selected conversation
- **Messages**: Messages in current conversation
- **Participants**: Map of user ID → User info
- **Typing Users**: Set of users currently typing

### Backend (Persistent)
- **Users**: User accounts with phone numbers
- **Conversations**: Conversation records
- **Messages**: Message history (7-day retention)
- **Read Status**: Message read receipts

## Configuration System

### Environment Variables
All configuration is environment-based (see `.env.local`):

```
API_URL           → API endpoint configuration
WS_URL            → WebSocket endpoint
TIMEOUT           → Request timeout
FEATURE FLAGS     → Enable/disable features
MESSAGE CONFIG    → Constraints and limits
```

### Config Module
The `config.ts` module:
- Loads environment variables
- Validates configuration on startup
- Provides typed access to all settings
- Used throughout the app via imports

## Error Handling

### API Errors
```
apiCall()
  ├─ Network error → throw Error
  ├─ 4xx response → parse error message, throw Error
  ├─ 5xx response → generic error, throw Error
  └─ Success → return parsed JSON

Caught by:
  ├─ Component try/catch → setError() state
  ├─ User feedback via error messages
  └─ Console logging (debug mode)
```

### WebSocket Errors
```
WebSocket connection fails
  ├─ Retry with exponential backoff (3s delay)
  ├─ Max 5 reconnection attempts
  ├─ Queue messages while disconnected
  ├─ Resume sending when reconnected
  └─ Show connection status to user
```

## Performance Optimizations

### Message Pagination
- Load 50 messages per request (configurable)
- Only store current conversation messages in memory
- Future: Implement infinite scroll with load more

### Component Optimization
- Split components into smaller pieces
- Use React.memo for list items (future)
- Avoid unnecessary re-renders with proper dependencies
- Context split: AuthContext + ChatContext (avoid single large context)

### Network Optimization
- Timeout on API calls (30s configurable)
- WebSocket message queuing for offline scenarios
- Debounced typing indicators (1s cooldown)

## Security Considerations

### Authentication
- JWT token in localStorage (standard for SPAs)
- Token sent in Authorization header for API requests
- Token passed in WebSocket URL for WS authentication
- No sensitive data in JWT payload (use server validation)

### Input Validation
- Message length enforced (max 5000 chars)
- Phone number format validation
- OTP format validation (6 digits)
- Input sanitization on backend

### WebSocket Security
- Token validation on WebSocket upgrade
- Message type validation
- Sender verification (ensure user owns conversation)
- Rate limiting on message sending (backend)

## Deployment Considerations

### Development
- All features enabled
- Debug logging on
- Local backend access
- Hot reload enabled

### Production
- Debug logging off (configurable)
- HTTPS + WSS (secure protocols)
- Proper CORS headers
- Error tracking integration
- Performance monitoring

## Testing Strategy

### Unit Testing (Future)
- API client functions
- Config validation
- Utility functions
- Type safety with TypeScript

### Integration Testing (Future)
- Auth flow end-to-end
- Message sending and receiving
- WebSocket connection and events
- Error handling scenarios

### Manual Testing
- Browser DevTools Console (check for [v0] logs)
- Network tab (verify API calls)
- WebSocket tab (verify event structure)
- Mobile responsiveness

## Extending the Application

### Adding New Features

1. **Database Schema Changes**: Modify types.ts
2. **New API Endpoints**: Add apiCall() wrapper
3. **UI Components**: Create in /components with props interface
4. **State Management**: Update ChatContext or create new context
5. **WebSocket Events**: Register in use-websocket.ts

### Feature Flags

Features can be controlled via environment variables:
- TYPING_INDICATORS
- READ_RECEIPTS
- PRESENCE
- DEBUG_LOGGING

Set to "false" to disable in UI (backend can still send, frontend ignores).

## Known Limitations

1. Messages load once per conversation (no infinite scroll yet)
2. No message search functionality
3. No group conversations (1-to-1 only)
4. No message encryption
5. No file/image sharing
6. localStorage auth (vulnerable on shared devices, use httpOnly cookies in production)

## Future Improvements

1. Implement message search
2. Add infinite scroll for messages
3. Support group conversations
4. Message reactions/emoji support
5. File and image sharing
6. User blocking/muting
7. Message encryption
8. Read-only mode for archived conversations
9. Message editing and deletion
10. Conversation pinning/favoriting
