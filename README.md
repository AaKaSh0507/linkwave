# Linkwave

Real-time chat application with session-based authentication, WebSocket messaging, and Kafka-backed persistence. Designed for scale and robustness.

## 🚀 Tech Stack

- **Backend:** Java 21, Spring Boot 3, Spring Security, Spring Data JPA
- **Messaging:** STOMP over WebSocket, Apache Kafka
- **Frontend:** Next.js 15 (App Router), React, TypeScript, Tailwind CSS
- **Storage:** PostgreSQL 17, Redis
- **Infra:** Docker, Kubernetes (k3s)

---

## 🛠️ Quick Start

The project uses a `Makefile` to simplify development workflows.

```bash
# 1. Install all dependencies (Frontend + Backend)
make install

# 2. Start all services in the background (Docker + Backend + Frontend)
make run

# 3. Access the application
# URL: http://localhost:3000
```

### Useful Commands
- `make stop` - Stop backend/frontend processes
- `make stop-all` - Stop everything including Docker containers
- `make dev` - Start only Docker services (PostgreSQL, Redis, Kafka, MailHog)
- `make status` - Check status of all services
- `make logs` - Follow all logs

---

## 🏗️ Architecture & Core Features

### Real-time Messaging
Linkwave uses **STOMP over WebSocket** for real-time communication, with a native WebSocket fallback at `/ws` for raw JSON messaging.

1. **Client Persistence:** Messages sent via `/app/chat.send` are validated and published to Kafka.
2. **Kafka Backbone:** The `chat.messages` topic handles decoupling of message ingestion and persistence.
3. **Database:** Messages are persisted in PostgreSQL for full history retrieval.
4. **Broadcast:** Consumers broadcast messages back to room subscribers via `/topic/room.{roomId}`.

### Advanced Features

#### 🔹 Typing Indicators
Ephemeral, in-memory system for real-time feedback.
- **Auto-timeout:** indicators clear after 5 seconds of inactivity.
- **Rate Limiting:** 2-second cooldown between typing events to prevent spam.
- **Isolation:** Strictly scoped to participants within a specific room.

#### 🔹 Read Receipts
Persistent tracking of message read status for 1-1 and group chats.
- **Idempotency:** Guaranteed at both application and database levels.
- **Batch Processing:** Supports marking up to 50 messages as read in a single operation.
- **Group Support:** Tracks multiple readers per message with "Seen by..." granularity.

---

## 📡 API & WebSocket Reference

### Authentication
- `POST /api/v1/auth/request-otp` - Request OTP via email.
- `POST /api/v1/auth/verify-otp` - Verify OTP and establish session.
- `POST /api/v1/auth/logout` - Invalidate session.

### Chat & Rooms
- `GET /api/v1/chat/rooms` - List user's active rooms.
- `POST /api/v1/chat/rooms/direct` - Start/Retrieve 1-1 chat.
- `POST /api/v1/chat/rooms/group` - Create group chat.
- `GET /api/v1/chat/rooms/{id}/messages` - Fetch message history.

### WebSocket Endpoints
- `STOMP: /ws/chat` - Primary messaging endpoint.
- `Native: /ws` - JSON-based fallback messaging.

---

## 🧪 Development

### Running Tests
```bash
make test  # Runs backend JUnit tests
```

### Database Access
```bash
make db-shell  # Direct access to PostgreSQL via psql
```

### Infrastructure
Kubernetes manifests are located in `k8s/`. The project is designed to run on `k3s` with `Traefik` as the Ingress controller.