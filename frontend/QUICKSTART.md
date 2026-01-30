# Quick Start Guide

Get the chat application running in 5 minutes.

## 1. Install Dependencies
```bash
npm install
```

## 2. Configure Environment
The `.env.local` file is already created with default settings for local development:
```bash
# View current settings
cat .env.local

# If you need to change backend URL:
# Edit .env.local and update NEXT_PUBLIC_API_URL and NEXT_PUBLIC_WS_URL
```

## 3. Start Backend
Before starting the frontend, ensure your Spring Boot backend is running:
```bash
# In another terminal, run your backend
cd /path/to/backend
./gradlew bootRun
# or: java -jar app.jar

# Test it's running:
curl http://localhost:8080/api/v1/health
```

## 4. Start Frontend
```bash
npm run dev
```

The app will start at `http://localhost:3000`

## 5. Test the App

### Phone Number to Use
Try any phone number format:
- `+1234567890`
- `(555) 123-4567`
- `5551234567`

### What to Expect

1. **Login Page** - Enter a phone number
2. **OTP Page** - Enter 6-digit code (check backend logs for the OTP or use test OTP from your backend)
3. **Chat Interface** - See empty conversation list
4. **Search Contacts** - Click "+ New" button to search for other users
5. **Send Message** - Once you have a conversation, type and send messages
6. **Real-time Updates** - Messages appear instantly via WebSocket

## Troubleshooting

### "Failed to fetch" or "Network Error"
```bash
# Check backend is running
curl http://localhost:8080/api/v1/health

# Check .env.local has correct API_URL
grep NEXT_PUBLIC_API_URL .env.local

# Verify it matches your backend URL
```

### "WebSocket connection failed"
```bash
# Open browser console (F12 → Console)
# Look for error messages starting with [v0]
# Check that WS_URL in .env.local matches backend

# Enable debug logging to see connection attempts:
# NEXT_PUBLIC_DEBUG_LOGGING=true in .env.local
```

### "OTP is invalid"
The OTP depends on your backend implementation. Common options:
- Use a fixed OTP for testing (e.g., "123456")
- Check backend logs for generated OTP
- Your backend may send OTP via email/SMS

## Development Tips

### Enable Debug Logging
To see all API calls and WebSocket events:
```env
NEXT_PUBLIC_DEBUG_LOGGING=true
```

Then open browser console (F12) and look for `[v0]` messages.

### Test with Multiple Tabs
1. Open app in Tab 1, login as User A
2. Open app in Tab 2 (Incognito mode), login as User B
3. In Tab 1: Search for User B's phone number
4. In Tab 2: Search for User A's phone number
5. Send messages between tabs - should update in real-time

### Common Environment Variables

| Variable | Dev Default | Production |
|----------|------------|-----------|
| API_URL | http://localhost:8080/api/v1 | https://api.yourserver.com/api/v1 |
| WS_URL | localhost:8080 | api.yourserver.com |
| DEBUG_LOGGING | true | false |
| API_TIMEOUT | 30000 | 10000 |
| MAX_MESSAGE_LENGTH | 5000 | 5000 |

## Next Steps

- Read [SETUP_GUIDE.md](./SETUP_GUIDE.md) for detailed configuration
- Read [INTEGRATION_CHECKLIST.md](./INTEGRATION_CHECKLIST.md) to verify backend endpoints
- Read [ARCHITECTURE.md](./ARCHITECTURE.md) to understand how it works
- Check [README.md](./README.md) for full feature list

## Support

### Check Browser Console
```javascript
// The app logs everything with [v0] prefix
// Example messages:
[v0] API Call: { url: '...', method: 'POST' }
[v0] WebSocket connected
[v0] API Response: { ... }
```

### Check Network Tab
1. Open DevTools (F12)
2. Go to Network tab
3. Send a message
4. Look for POST request to `/conversations/{id}/messages`
5. Check response body and headers

### Check WebSocket Events
1. Open DevTools (F12)
2. Go to Network tab, find "WS" entries
3. Click on the WebSocket entry
4. Go to Messages tab
5. See all messages sent/received

## Production Deployment

When ready to deploy:

1. Update `.env.local` with production URLs:
   ```env
   NEXT_PUBLIC_API_URL=https://api.yourserver.com/api/v1
   NEXT_PUBLIC_WS_URL=api.yourserver.com
   NEXT_PUBLIC_DEBUG_LOGGING=false
   ```

2. Build:
   ```bash
   npm run build
   ```

3. Start:
   ```bash
   npm start
   ```

4. Deploy to Vercel, Docker, or your hosting platform

## Feature Overview

- ✅ Phone-based OTP authentication
- ✅ Real-time messaging via WebSocket
- ✅ Online/offline status indicators
- ✅ Typing indicators (when enabled)
- ✅ Read receipts (when enabled)
- ✅ Contact search
- ✅ Conversation list with unread counts
- ✅ Message pagination
- ✅ Character count with max length validation

All features are controlled by environment variables and can be enabled/disabled without code changes.

---

For more details, see the documentation files in this repository.
