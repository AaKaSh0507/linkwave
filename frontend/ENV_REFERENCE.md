# Environment Variables Reference

Complete documentation of all environment variables used by the frontend application.

## API Configuration

### NEXT_PUBLIC_API_URL
**Type**: `string`  
**Default**: `http://localhost:8080/api/v1`  
**Description**: Base URL for all REST API requests

**Examples**:
- Local development: `http://localhost:8080/api/v1`
- Remote server: `https://api.example.com/api/v1`
- Docker: `http://backend:8080/api/v1`

**Used by**:
- `lib/config.ts` - Configures base URL for apiCall()
- `lib/api.ts` - Constructs full API URLs

**Note**: Must include `/api/v1` path, not just the host

### NEXT_PUBLIC_API_TIMEOUT
**Type**: `number` (milliseconds)  
**Default**: `30000` (30 seconds)  
**Description**: How long to wait for API responses before timing out

**Examples**:
- Fast network: `10000` (10 seconds)
- Slow/unreliable: `60000` (1 minute)
- Development/debugging: `30000`

**Used by**:
- `lib/api.ts` - AbortController timeout in apiCall()

**Note**: If requests take longer than this, they will fail

## WebSocket Configuration

### NEXT_PUBLIC_WS_URL
**Type**: `string`  
**Default**: `localhost:8080`  
**Description**: Base URL for WebSocket connection (without protocol or path)

**Examples**:
- Local development: `localhost:8080`
- Remote server: `api.example.com`
- Docker: `backend:8080`

**Used by**:
- `lib/config.ts` - getWebSocketUrl() constructs full WebSocket URL
- Final URL: `ws://localhost:8080/chat` (or `wss://` for HTTPS)

**Note**: Do NOT include `ws://`, `http://`, protocol, or `/chat` path

### NEXT_PUBLIC_WS_RECONNECT_DELAY
**Type**: `number` (milliseconds)  
**Default**: `3000` (3 seconds)  
**Description**: Wait time between reconnection attempts

**Examples**:
- Fast recovery: `1000` (1 second)
- Normal: `3000` (3 seconds)
- Slow: `5000` (5 seconds)

**Used by**:
- `lib/websocket.ts` - setTimeout() delay between reconnects

**Note**: Delay increases if using exponential backoff (future enhancement)

### NEXT_PUBLIC_WS_MAX_RECONNECT
**Type**: `number`  
**Default**: `5`  
**Description**: Maximum number of reconnection attempts before giving up

**Examples**:
- Strict (fail fast): `1` or `2`
- Normal: `5`
- Persistent: `10`

**Used by**:
- `lib/websocket.ts` - Check reconnectAttempts < maxReconnectAttempts

**Note**: After reaching this limit, connection will not retry

## Feature Flags

These control which features are enabled. Set to `"true"` to enable, `"false"` to disable.

### NEXT_PUBLIC_DEBUG_LOGGING
**Type**: `"true"` or `"false"`  
**Default**: `true`  
**Description**: Enable console logging for debugging

**What gets logged**:
- API requests and responses
- WebSocket connection events
- Authentication flow
- Message sending/receiving
- Error details

**Used by**:
- `lib/api.ts` - Log API calls
- `lib/websocket.ts` - Log WebSocket events
- `lib/auth-context.tsx` - Log auth events
- All components with `config.features.enableDebugLogging` checks

**Production Setting**: `false` (disable console spam)

**Examples**:
```env
# Development
NEXT_PUBLIC_DEBUG_LOGGING=true

# Production
NEXT_PUBLIC_DEBUG_LOGGING=false
```

### NEXT_PUBLIC_TYPING_INDICATORS
**Type**: `"true"` or `"false"`  
**Default**: `true`  
**Description**: Show when someone is typing a message

**Impact**:
- Enabled: Shows animated dots when typing
- Disabled: No typing indicators in UI, but backend may still send events (ignored)

**Used by**:
- `lib/use-websocket.ts` - Register TYPING listener
- `components/chat/message-thread.tsx` - Show TypingIndicator component
- `components/chat/message-input.tsx` - Send typing events

**Examples**:
```env
# Enable typing indicators
NEXT_PUBLIC_TYPING_INDICATORS=true

# Disable if backend doesn't support
NEXT_PUBLIC_TYPING_INDICATORS=false
```

### NEXT_PUBLIC_READ_RECEIPTS
**Type**: `"true"` or `"false"`  
**Default**: `true`  
**Description**: Show message read status (✓ and ✓✓)

**Impact**:
- Enabled: Shows read receipts on sent messages
- Disabled: No read status indicators

**Used by**:
- `lib/use-websocket.ts` - Register READ_RECEIPT listener
- `lib/chat-context.tsx` - Handle read receipt updates
- `components/chat/message-bubble.tsx` - Show read status (future implementation)

**Examples**:
```env
# Enable read receipts
NEXT_PUBLIC_READ_RECEIPTS=true

# Disable if not needed
NEXT_PUBLIC_READ_RECEIPTS=false
```

### NEXT_PUBLIC_PRESENCE
**Type**: `"true"` or `"false"`  
**Default**: `true`  
**Description**: Show online/offline status and last seen time

**Impact**:
- Enabled: Shows user status in conversations
- Disabled: No presence information displayed

**Used by**:
- `lib/use-websocket.ts` - Register PRESENCE listener
- `lib/chat-context.tsx` - Update presence status
- `components/chat/conversation-list.tsx` - Show status (future implementation)

**Examples**:
```env
# Enable presence
NEXT_PUBLIC_PRESENCE=true

# Disable for privacy
NEXT_PUBLIC_PRESENCE=false
```

## Message Configuration

### NEXT_PUBLIC_MAX_MESSAGE_LENGTH
**Type**: `number` (characters)  
**Default**: `5000`  
**Description**: Maximum length of a single message

**Impact**:
- User can't type more than this length
- Input enforced in MessageInput component
- Shows character counter when near limit

**Used by**:
- `components/chat/message-input.tsx` - Prevent input > max length
- Validation in UI (character counter)

**Must match backend constraint** - if backend enforces 1000 and frontend allows 5000, messages will fail to send

**Examples**:
```env
# Standard
NEXT_PUBLIC_MAX_MESSAGE_LENGTH=5000

# Shorter for SMS-like
NEXT_PUBLIC_MAX_MESSAGE_LENGTH=160

# Longer for rich conversations
NEXT_PUBLIC_MAX_MESSAGE_LENGTH=10000
```

### NEXT_PUBLIC_MESSAGE_RETENTION_DAYS
**Type**: `number` (days)  
**Default**: `7`  
**Description**: How long messages are kept before auto-deletion

**Impact**:
- Display information about message retention
- Frontend informational only (backend enforces)
- Used in tooltips/help text

**Note**: This is mostly informational. Backend actually enforces deletion.

**Examples**:
```env
# 7-day retention (default)
NEXT_PUBLIC_MESSAGE_RETENTION_DAYS=7

# 30-day retention
NEXT_PUBLIC_MESSAGE_RETENTION_DAYS=30

# 1-day retention (ephemeral)
NEXT_PUBLIC_MESSAGE_RETENTION_DAYS=1
```

### NEXT_PUBLIC_MESSAGE_PAGE_SIZE
**Type**: `number` (messages per request)  
**Default**: `50`  
**Description**: How many messages to load per API request

**Impact**:
- Larger = fewer requests, slower first load
- Smaller = faster first load, more requests

**Used by**:
- `lib/chat-context.tsx` - fetchMessages() adds `?limit=` param
- Backend should respect this limit parameter

**Optimization**:
- Start with 50
- Increase to 100 if messages are small
- Decrease to 20 if very large messages/slow network

**Examples**:
```env
# Conservative (fast first load)
NEXT_PUBLIC_MESSAGE_PAGE_SIZE=20

# Standard
NEXT_PUBLIC_MESSAGE_PAGE_SIZE=50

# Aggressive (load more at once)
NEXT_PUBLIC_MESSAGE_PAGE_SIZE=100
```

## Node Environment

### NODE_ENV
**Type**: `"development"` or `"production"`  
**Default**: `development`  
**Description**: Node.js environment mode

**Impact**:
- Development: Hot reload, debug info, larger bundles
- Production: Optimized bundles, no debug info

**Set by**:
- `npm run dev` → `development`
- `npm run build` + `npm start` → `production`

**Note**: Should match your deployment environment

## Environment-Specific Examples

### Development Setup
```env
# .env.local for local development
NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_API_TIMEOUT=30000
NEXT_PUBLIC_WS_URL=localhost:8080
NEXT_PUBLIC_WS_RECONNECT_DELAY=3000
NEXT_PUBLIC_WS_MAX_RECONNECT=5
NEXT_PUBLIC_DEBUG_LOGGING=true
NEXT_PUBLIC_TYPING_INDICATORS=true
NEXT_PUBLIC_READ_RECEIPTS=true
NEXT_PUBLIC_PRESENCE=true
NEXT_PUBLIC_MAX_MESSAGE_LENGTH=5000
NEXT_PUBLIC_MESSAGE_RETENTION_DAYS=7
NEXT_PUBLIC_MESSAGE_PAGE_SIZE=50
NODE_ENV=development
```

### Staging Setup
```env
# Staging environment (still has debug logging)
NEXT_PUBLIC_API_URL=https://api-staging.example.com/api/v1
NEXT_PUBLIC_API_TIMEOUT=20000
NEXT_PUBLIC_WS_URL=api-staging.example.com
NEXT_PUBLIC_WS_RECONNECT_DELAY=3000
NEXT_PUBLIC_WS_MAX_RECONNECT=5
NEXT_PUBLIC_DEBUG_LOGGING=true
NEXT_PUBLIC_TYPING_INDICATORS=true
NEXT_PUBLIC_READ_RECEIPTS=true
NEXT_PUBLIC_PRESENCE=true
NEXT_PUBLIC_MAX_MESSAGE_LENGTH=5000
NEXT_PUBLIC_MESSAGE_RETENTION_DAYS=7
NEXT_PUBLIC_MESSAGE_PAGE_SIZE=50
NODE_ENV=production
```

### Production Setup
```env
# Production environment (optimized)
NEXT_PUBLIC_API_URL=https://api.example.com/api/v1
NEXT_PUBLIC_API_TIMEOUT=10000
NEXT_PUBLIC_WS_URL=api.example.com
NEXT_PUBLIC_WS_RECONNECT_DELAY=5000
NEXT_PUBLIC_WS_MAX_RECONNECT=3
NEXT_PUBLIC_DEBUG_LOGGING=false
NEXT_PUBLIC_TYPING_INDICATORS=true
NEXT_PUBLIC_READ_RECEIPTS=true
NEXT_PUBLIC_PRESENCE=true
NEXT_PUBLIC_MAX_MESSAGE_LENGTH=5000
NEXT_PUBLIC_MESSAGE_RETENTION_DAYS=7
NEXT_PUBLIC_MESSAGE_PAGE_SIZE=50
NODE_ENV=production
```

## How to Change Variables

### 1. Edit .env.local
```bash
# Open the file
nano .env.local
# or
vi .env.local
# or
open .env.local  # macOS

# Make changes and save
# Restart dev server for changes to take effect
npm run dev
```

### 2. Command Line Override
```bash
# Set for current session only
NEXT_PUBLIC_API_URL=http://newhost:8080/api/v1 npm run dev

# Or for build
NEXT_PUBLIC_DEBUG_LOGGING=false npm run build
```

### 3. CI/CD Pipeline
```bash
# GitHub Actions, GitLab CI, etc.
- name: Build
  env:
    NEXT_PUBLIC_API_URL: https://api.example.com/api/v1
    NEXT_PUBLIC_DEBUG_LOGGING: false
  run: npm run build
```

## Validation

The app validates configuration on startup:

```typescript
// lib/config.ts
validateConfig() {
  // Checks:
  // - NEXT_PUBLIC_API_URL is set
  // - Numbers can be parsed
  // - Feature flags are boolean values
}
```

If validation fails in development, app continues with defaults. In production, you should see console warnings.

## Accessing Config in Code

```typescript
// Import config
import { config } from '@/lib/config'

// Access values
console.log(config.api.baseUrl)
console.log(config.websocket.url)
console.log(config.features.enableDebugLogging)
console.log(config.messages.maxLength)

// Get WebSocket URL
import { getWebSocketUrl } from '@/lib/config'
const wsUrl = getWebSocketUrl() // Returns: ws://localhost:8080/chat
```

## Common Issues

### Environment variables not updating
- Restart dev server: `npm run dev`
- Verify variables are prefixed with `NEXT_PUBLIC_`
- Check .env.local file is saved

### Settings not taking effect in production
- Run `npm run build` after changing vars
- Redeploy the built output
- Clear browser cache
- Check environment variables are set in production environment

### WebSocket connection fails with wrong URL
- Verify NEXT_PUBLIC_WS_URL doesn't include `ws://`
- Verify NEXT_PUBLIC_WS_URL doesn't include `/chat`
- Check browser console for actual URL being used

---

See also: [QUICKSTART.md](./QUICKSTART.md), [SETUP_GUIDE.md](./SETUP_GUIDE.md)
