# Linkwave

Modern realtime chat application with session-based authentication.

## Status

**Implemented:**
- Session-based authentication with Redis
- OTP verification via email
- CSRF protection
- Vue 3 frontend with protected routes

**Not Yet Implemented:**
- Real-time messaging (WebSocket)
- User persistence
- Contact management
- Message history

## Tech Stack

**Backend:** Java 21, Spring Boot 3.4.1, Spring Security, Redis, PostgreSQL, Gradle  
**Frontend:** Vue 3.5, Vuetify 3.7, Pinia 2.2, Vite 5.4, Axios  
**Infrastructure:** Docker, Kafka, Mailhog

## Quick Start

### 1. Start Infrastructure

```bash
docker compose up -d
```

### 2. Start Backend

```bash
cd backend
./gradlew build
./gradlew bootRun
```

Backend: http://localhost:8080

### 3. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend: http://localhost:5173

### 4. Test OTP Flow

1. Open http://localhost:5173
2. Enter phone (+1234567890) and email
3. Check OTP in Mailhog: http://localhost:8025
4. Enter OTP to login
5. Session persists across refresh

## Configuration

**Backend:** `backend/src/main/resources/application.yml`  
Environment variables: DATABASE_URL, REDIS_HOST, MAIL_HOST, etc.

**Frontend:** Create `frontend/.env.local`:
```env
VITE_API_BASE_URL=http://localhost:8080
```

**Infrastructure:** Root `.env` file (see `.env.example` for reference)

## Development

```bash
# Backend tests
cd backend && ./gradlew test

# Frontend build
cd frontend && npm run build
```

## Deployment

See [DOCKER_SETUP.md](DOCKER_SETUP.md) and [CLUSTER_SETUP.md](CLUSTER_SETUP.md) for container and Kubernetes deployment.

## License

Proprietary

