# Setup Guide - Realtime Chat Application

This guide will walk you through setting up the frontend application to work with your Spring Boot backend.

## Prerequisites

- Node.js 18+ (verify with `node --version`)
- npm or yarn (verify with `npm --version`)
- Backend API running and accessible
- Backend WebSocket server running and accessible

## Step 1: Clone and Install

```bash
# Clone the repository
git clone <your-repo-url>
cd realtime-chat-app

# Install dependencies
npm install
```

## Step 2: Configure Environment Variables

The application requires specific environment variables to connect to your backend. A `.env.local` file has been created with default values for local development.

### Edit `.env.local`

Update these values to match your backend configuration:

```env
# API Configuration - Update if backend is on different host/port
NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_API_TIMEOUT=30000

# WebSocket Configuration - Update if WebSocket is on different host/port
NEXT_PUBLIC_WS_URL=localhost:8080

# Feature Flags - Control which features are enabled
NEXT_PUBLIC_DEBUG_LOGGING=true           # Enable console logging for debugging
NEXT_PUBLIC_TYPING_INDICATORS=true       # Show when someone is typing
NEXT_PUBLIC_READ_RECEIPTS=true           # Show message read status
NEXT_PUBLIC_PRESENCE=true                # Show online/offline status

# Message Configuration - Must match backend settings
NEXT_PUBLIC_MAX_MESSAGE_LENGTH=5000      # Maximum characters per message
NEXT_PUBLIC_MESSAGE_RETENTION_DAYS=7     # How long messages are kept
NEXT_PUBLIC_MESSAGE_PAGE_SIZE=50         # Messages to load per request
```

### Common Configuration Scenarios

**Local Development (Default)**
```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_WS_URL=localhost:8080
NEXT_PUBLIC_DEBUG_LOGGING=true
```

**Remote Server with HTTPS**
```env
NEXT_PUBLIC_API_URL=https://api.yourserver.com/api/v1
NEXT_PUBLIC_WS_URL=api.yourserver.com
NEXT_PUBLIC_DEBUG_LOGGING=false
```

**Docker Container**
```env
NEXT_PUBLIC_API_URL=http://backend:8080/api/v1
NEXT_PUBLIC_WS_URL=backend:8080
NEXT_PUBLIC_DEBUG_LOGGING=true
```

## Step 3: Verify Backend API

Before running the frontend, ensure your backend is running and accessible:

```bash
# Test API endpoint
curl http://localhost:8080/api/v1/health

# You should get a 200 response if backend is running
```

The frontend expects these API endpoints to exist:

- `POST /auth/request-otp` - Send OTP to phone number
- `POST /auth/verify-otp` - Verify OTP and return token
- `GET /conversations` - Get user's conversations
- `POST /conversations` - Create new conversation
- `GET /conversations/{id}/messages` - Get messages (with limit query param)
- `POST /conversations/{id}/messages` - Send message
- `GET /users/search` - Search users by phone

## Step 4: Start Development Server

```bash
npm run dev
```

The application will start at `http://localhost:3000`

### Development Server Features

- Hot reload on file changes
- Console logging enabled by default
- Mock data available if needed

## Step 5: Test the Application

1. Open `http://localhost:3000` in your browser
2. Enter any phone number to request OTP
3. Check your backend logs - it should receive the request
4. Enter a 6-digit OTP to verify (backend-dependent)
5. After authentication, you should see the chat interface
6. Open the browser console (F12) to see debug logs

## Troubleshooting

### "API Error: 404" or "Failed to fetch"

**Problem**: Frontend can't reach the backend API

**Solution**:
1. Verify backend is running: `curl http://localhost:8080`
2. Check `NEXT_PUBLIC_API_URL` in `.env.local`
3. Check CORS headers in backend
4. Look at browser console (F12) for actual error messages

### "WebSocket connection failed"

**Problem**: WebSocket connection can't be established

**Solution**:
1. Verify WebSocket is enabled in backend
2. Check `NEXT_PUBLIC_WS_URL` in `.env.local`
3. In browser console, you should see WebSocket URL being used
4. Check backend WebSocket logs

### "Maximum reconnection attempts reached"

**Problem**: WebSocket keeps disconnecting

**Solution**:
1. Check backend WebSocket handler is working
2. Verify token is valid and sent correctly
3. Check network tab in DevTools for WebSocket frame errors
4. Increase `NEXT_PUBLIC_WS_RECONNECT_DELAY` if server is slow

### "Blank page after login"

**Problem**: Chat page doesn't load after authentication

**Solution**:
1. Check browser console for errors (F12)
2. Check Network tab to see API calls
3. Verify conversations endpoint is returning data
4. Check that `fetchConversations` is being called

### Debug Logging

To get more detailed information:

1. Set `NEXT_PUBLIC_DEBUG_LOGGING=true` in `.env.local`
2. Open browser DevTools Console (F12)
3. Look for `[v0]` prefixed messages
4. These show API calls, WebSocket events, and state changes

## Production Build

When ready to deploy:

```bash
# Build optimized production bundle
npm run build

# Start production server
npm start
```

The production build will:
- Optimize code and assets
- Disable debug logging (unless explicitly enabled)
- Ready for deployment to Vercel, Docker, or other platforms

## Environment-Specific Configuration

### Development
- `NEXT_PUBLIC_DEBUG_LOGGING=true` - See all debug messages
- `NEXT_PUBLIC_API_TIMEOUT=30000` - Generous timeout for slow machines

### Production
- `NEXT_PUBLIC_DEBUG_LOGGING=false` - No console spam
- `NEXT_PUBLIC_API_TIMEOUT=10000` - Strict timeout for user experience
- Set `NODE_ENV=production`

## Backend Integration Checklist

Before going live, ensure your backend:

- [ ] Accepts OTP requests at `POST /auth/request-otp`
- [ ] Returns `{ otpId: string }` from OTP endpoint
- [ ] Accepts verification at `POST /auth/verify-otp`
- [ ] Returns `{ token: string, user: { id, phoneNumber, displayName? } }`
- [ ] Implements all conversation endpoints with correct pagination
- [ ] Sends WebSocket messages with correct event types
- [ ] Validates JWT tokens from Authorization header
- [ ] Implements proper CORS headers
- [ ] Has WebSocket endpoint at `/chat` path
- [ ] Accepts token as query parameter in WebSocket URL

## Support

If you encounter issues:

1. Check browser console for error messages
2. Check backend logs for request/response details
3. Verify all environment variables are set correctly
4. Ensure backend API and WebSocket are both accessible
5. Test API endpoints manually with curl or Postman

For more information, see the main README.md file.
