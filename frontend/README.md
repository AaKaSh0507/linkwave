# Realtime Chat Application

A modern, production-ready realtime messaging frontend built with Next.js 16, React 19, and TypeScript. This application provides seamless one-to-one messaging with real-time updates, typing indicators, presence status, and read receipts.

## Features

- **OTP-based Authentication**: Secure phone number verification via OTP
- **Real-time Messaging**: WebSocket-powered instant message delivery
- **Presence Indicators**: See who's online and when they were last active
- **Typing Indicators**: Know when someone is typing a message
- **Read Receipts**: Track message delivery and read status
- **Contact Search**: Easily find and start conversations with contacts
- **Responsive Design**: Fully mobile-responsive interface
- **Modern UI**: Clean, intuitive design with smooth animations

## Tech Stack

- **Framework**: Next.js 16 with App Router
- **Runtime**: React 19 with Server Components
- **Language**: TypeScript
- **Styling**: Tailwind CSS v4
- **UI Components**: shadcn/ui
- **Real-time**: Native WebSocket API
- **Date Handling**: date-fns

## Project Structure

```
/app
  /auth           # Authentication pages
  /chat           # Chat interface
  layout.tsx      # Root layout with providers
  page.tsx        # Redirect to chat

/components
  /auth           # Auth-related components
    login-form.tsx
    otp-verification.tsx
  /chat           # Chat-related components
    conversation-list.tsx
    message-bubble.tsx
    message-input.tsx
    message-thread.tsx
    contact-search.tsx
    typing-indicator.tsx
  /ui             # shadcn/ui components

/lib
  api.ts          # API client utilities
  auth-context.tsx # Authentication state management
  chat-context.tsx # Chat state management
  types.ts        # TypeScript type definitions
  websocket.ts    # WebSocket client
  use-websocket.ts # WebSocket React hook
  config.ts       # Configuration management
  utils.ts        # Utility functions

/public           # Static assets

.env.example      # Environment variable template
```

## Getting Started

### Prerequisites

- Node.js 18+ and npm/yarn
- A backend API running at `http://localhost:8080/api/v1` (development)
- WebSocket server at `ws://localhost:8080/chat` (development)

### Installation

1. Clone the repository:

```bash
git clone <repository-url>
cd realtime-chat-app
```

2. Install dependencies:

```bash
npm install
```

3. Configure environment variables:

```bash
cp .env.example .env.local
```

Update `.env.local` with your actual values:

```env
# API Configuration
NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_API_TIMEOUT=30000

# WebSocket Configuration
NEXT_PUBLIC_WS_URL=localhost:8080
NEXT_PUBLIC_WS_RECONNECT_DELAY=3000
NEXT_PUBLIC_WS_MAX_RECONNECT=5

# Feature Flags
NEXT_PUBLIC_DEBUG_LOGGING=true
NEXT_PUBLIC_TYPING_INDICATORS=true
NEXT_PUBLIC_READ_RECEIPTS=true
NEXT_PUBLIC_PRESENCE=true

# Message Configuration
NEXT_PUBLIC_MAX_MESSAGE_LENGTH=5000
NEXT_PUBLIC_MESSAGE_RETENTION_DAYS=7
NEXT_PUBLIC_MESSAGE_PAGE_SIZE=50
```

### Development

Start the development server:

```bash
npm run dev
```

The application will be available at `http://localhost:3000`

### Building for Production

Build the application:

```bash
npm run build
```

Start the production server:

```bash
npm start
```

## API Integration

The frontend expects a backend API with the following endpoints:

### Authentication

- `POST /auth/request-otp` - Request OTP for phone number
- `POST /auth/verify-otp` - Verify OTP and get auth token

### Conversations

- `GET /conversations` - Get user's conversations
- `POST /conversations` - Create new conversation
- `GET /conversations/{id}/messages?limit=50` - Get messages for conversation (with paging)
- `POST /conversations/{id}/messages` - Send message

### Users

- `GET /users/search?q=query` - Search for users by phone number

## WebSocket Events

The application expects WebSocket connection at the configured URL with the following event types:

### Server → Client

- `MESSAGE` - New message in conversation
- `TYPING` - User typing indicator
- `PRESENCE` - User presence update (online/offline)
- `READ_RECEIPT` - Message read confirmation

### Client → Server

- `SEND_MESSAGE` - Send a new message
- `TYPING` - Send typing indicator
- `READ_RECEIPT` - Send read receipt

## Environment Configuration

All configuration is managed through environment variables. See `.env.example` for complete list:

- **API Configuration**: Base URL and timeout settings
- **WebSocket Configuration**: Server URL and reconnection settings
- **Feature Flags**: Enable/disable specific features
- **Message Configuration**: Message length and retention settings

## Authentication Flow

1. User enters phone number on login page
2. OTP is sent to their email (via backend)
3. User enters 6-digit OTP within 5 minutes
4. Backend returns auth token and user info
5. Token stored in localStorage and used for API requests

## State Management

The app uses React Context API for state management:

- **AuthContext**: User authentication state
- **ChatContext**: Conversations, messages, and participants

## Real-time Features

### WebSocket Connection

The WebSocket client automatically:
- Connects on app load with auth token
- Reconnects on disconnect (up to 5 attempts)
- Queues messages while disconnected
- Emits typed events to listeners

### Typing Indicators

When user types in message input:
1. Typing indicator sent to WebSocket
2. Debounced after 1 second of inactivity
3. Displayed for other participants in real-time

### Presence Updates

User presence is:
- Set to "online" on app load
- Updated via WebSocket presence events
- Set to "offline" on logout
- Displayed with visual indicator

## Performance Optimizations

- Message virtualization for large conversations
- Lazy loading of conversations
- Efficient WebSocket event handling
- Optimized re-renders with React memo
- CSS animations for smooth interactions

## Security Considerations

- Auth tokens stored in localStorage
- API requests include Authorization header
- WebSocket authentication via token
- Input sanitization on message display
- No sensitive data in browser console (production)

## Troubleshooting

### WebSocket Connection Issues

If WebSocket fails to connect:
1. Check `NEXT_PUBLIC_WS_URL` configuration
2. Verify backend WebSocket server is running
3. Check browser console for connection errors
4. Try clearing cookies and reloading

### API Connection Issues

If API requests fail:
1. Verify `NEXT_PUBLIC_API_URL` is correct
2. Check backend server is running
3. Verify CORS headers are configured
4. Check auth token is valid

### Messages Not Loading

If messages don't appear:
1. Verify WebSocket is connected
2. Check conversation ID is correct
3. Look for errors in browser console
4. Verify backend message retrieval endpoint

## Deployment

### Deploy to Vercel

1. Push code to GitHub repository
2. Connect repository to Vercel
3. Set environment variables in Vercel dashboard
4. Deploy with `npm run build && npm start`

### Deploy to Other Platforms

Ensure your deployment platform:
- Supports Node.js 18+
- Allows WebSocket connections
- Has environment variable configuration
- Runs `npm run build` as build step
- Runs `npm start` as start command

## Contributing

1. Create a feature branch
2. Make your changes
3. Submit a pull request

## License

MIT
