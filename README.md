# Linkwave

Real-time chat application with session-based authentication and WebSocket messaging.

## Tech Stack

**Backend:** Java 21, Spring Boot, Spring Security, Spring Data JPA, STOMP, Kafka  
**Frontend:** Next.js App Router, React, TypeScript, Tailwind CSS  
**Database:** PostgreSQL, Redis  
**Infrastructure:** Docker, Kubernetes

## Quick Start

```bash
# Install dependencies
make install

# Start all services (Docker + Backend + Frontend)
make run
```

Access the app at `http://localhost:3000`

**Other useful commands:**
```bash
make stop          # Stop backend/frontend
make stop-all      # Stop everything including Docker
make dev           # Start Docker only (run backend/frontend in separate terminals)
make backend-fg    # Run backend in foreground
make frontend-fg   # Run frontend in foreground
make help          # See all available commands
```

## Features

- **Authentication:** OTP-based login via email
- **Real-time Chat:** STOMP over WebSocket with Kafka message queue
- **Room-based Messaging:** Direct (1-1) and group chats
- **Session Management:** Redis-backed HTTP sessions
- **Message Persistence:** PostgreSQL with full history

## API Endpoints

**Auth:**
- `POST /api/v1/auth/request-otp` - Request OTP
- `POST /api/v1/auth/verify-otp` - Verify OTP and create session
- `POST /api/v1/auth/logout` - Logout

**Chat:**
- `GET /api/v1/chat/rooms` - List user's rooms
- `POST /api/v1/chat/rooms/direct` - Create direct chat
- `POST /api/v1/chat/rooms/group` - Create group chat
- `GET /api/v1/chat/rooms/{id}/messages` - Get room messages
- `WebSocket /ws/chat` - STOMP messaging endpoint

## Development

**Run tests:**
```bash
make test
```

**Database access:**
```bash
make db-shell
```

**View logs:**
```bash
make logs
```

**Check service status:**
```bash
make status
```

## Architecture

```
Browser → Next.js → Spring Boot → Kafka → PostgreSQL
          ↓                       ↓
       WebSocket              Redis (sessions)
```

**Message Flow:**
1. Client sends message via STOMP (`/app/chat.send`)
2. Backend validates and publishes to Kafka topic `chat.messages`
3. Consumer persists to PostgreSQL
4. Consumer broadcasts to room subscribers (`/topic/room.{roomId}`)
5. All room members receive message in real-time

## Deployment

Kubernetes manifests in `k8s/` directory. Uses k3s + Traefik + cert-manager.

**Requirements:** 2 CPU, 4GB RAM, 40GB disk