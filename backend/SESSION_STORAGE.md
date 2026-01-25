# Redis Session Storage - Configuration Guide

## Overview

The Linkwave backend now uses Redis for server-side session storage, enabling stateless horizontal scaling. Sessions are stored in Redis instead of in-memory, allowing any backend instance to handle any request.

## Components

### Configuration Classes

- **[RedisConfig.java](src/main/java/com/linkwave/app/config/RedisConfig.java)** - Redis connection settings
- **[SessionConfig.java](src/main/java/com/linkwave/app/config/SessionConfig.java)** - Spring Session Redis configuration
- **[SecurityConfig.java](src/main/java/com/linkwave/app/config/SecurityConfig.java)** - Security and session management

### Domain Objects

- **[SessionMetadata.java](src/main/java/com/linkwave/app/domain/session/SessionMetadata.java)** - Session metadata wrapper

### Services

- **[SessionService.java](src/main/java/com/linkwave/app/service/session/SessionService.java)** - Session lifecycle management

## Configuration

### Environment Variables

```bash
# Redis Connection
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=changeme

# Session Settings
SESSION_TIMEOUT_MINUTES=30
SESSION_NAMESPACE=linkwave:session:
SESSION_TIMEOUT=30m

# Cookie Settings (Production)
COOKIE_SECURE=true
COOKIE_SAME_SITE=strict
```

### application.yml

Sessions are configured in [application.yml](src/main/resources/application.yml):

```yaml
spring:
  session:
    store-type: redis
    timeout: ${SESSION_TIMEOUT:30m}
    redis:
      namespace: ${SESSION_NAMESPACE:linkwave:session:}

server:
  servlet:
    session:
      cookie:
        name: LINKWAVE_SESSION
        http-only: true
        secure: ${COOKIE_SECURE:false}
        same-site: ${COOKIE_SAME_SITE:lax}
```

## Security Features

### Cookie Configuration

- **HTTPOnly**: Prevents JavaScript access to session cookie
- **Secure**: Cookie only sent over HTTPS (configurable for dev/prod)
- **SameSite**: CSRF protection (lax for development, strict for production)

### CSRF Protection

- Enabled for all POST endpoints except `/api/v1/auth/request-otp`
- OTP endpoint uses email-only flow, no browser form submission

### Session Policy

- Session created only when needed (`IF_REQUIRED`)
- Maximum 1 session per user (when authentication is added)
- Sessions persist across backend restarts (stored in Redis)

## Usage

### SessionService Methods

```java
// Create new session (future hook for post-OTP verification)
SessionMetadata metadata = sessionService.createSessionFor(phoneNumber);

// Get current session metadata
Optional<SessionMetadata> metadata = sessionService.getCurrentSessionMetadata();

// Store attribute in session
sessionService.setSessionAttribute("key", value);

// Retrieve attribute from session
Optional<Object> value = sessionService.getSessionAttribute("key");

// Invalidate current session
sessionService.invalidateSession();
```

## Development Setup

### 1. Start Redis

```bash
# Using docker-compose
docker-compose up redis-cache

# Or standalone
docker run -d -p 6379:6379 redis:7.4.2-alpine --requirepass changeme
```

### 2. Configure Environment

Copy `.env.example` to `.env` and configure Redis settings.

### 3. Run Backend

```bash
cd backend
./gradlew bootRun
```

## Testing

### Unit Tests

```bash
./gradlew test --tests SessionServiceTest
```

### Manual Testing

```bash
# Request OTP (creates session)
curl -X POST http://localhost:8080/api/v1/auth/request-otp \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber": "+1234567890", "email": "user@example.com"}' \
  -c cookies.txt

# Check session cookie in cookies.txt
cat cookies.txt

# Make another request with session cookie
curl http://localhost:8080/actuator/health -b cookies.txt
```

### Redis Inspection

```bash
# Connect to Redis
redis-cli -h localhost -p 6379 -a changeme

# List session keys
KEYS linkwave:session:*

# View session data
GET linkwave:session:sessions:<session-id>
```

## Production Considerations

### Environment Variables

Set these for production:

```bash
REDIS_HOST=redis-cluster.prod.internal
REDIS_PORT=6379
REDIS_PASSWORD=<strong-password>
SESSION_TIMEOUT_MINUTES=30
COOKIE_SECURE=true
COOKIE_SAME_SITE=strict
```

### Redis Configuration

- Use Redis Cluster or Sentinel for high availability
- Enable persistence (AOF + RDB)
- Configure memory limits and eviction policies
- Use TLS for Redis connections in production

### Monitoring

- Monitor session count: `redis-cli DBSIZE`
- Monitor session expiry: Check `linkwave:session:expirations`
- Track session creation/invalidation via application logs

## Future Enhancements

- **B4**: Link OTP verification to session (store phoneNumber after verification)
- **B5**: Add login endpoint that validates OTP and creates authenticated session
- **B6**: Add logout endpoint
- **B7**: Session-based authorization rules
- **WebSocket**: Session continuity for real-time chat

## Troubleshooting

### Session not persisting

- Verify Redis is running: `redis-cli ping`
- Check Redis connection in logs
- Verify `REDIS_PASSWORD` matches docker-compose

### Cookie not being set

- Check `COOKIE_SECURE` setting (should be false for HTTP development)
- Verify browser allows cookies
- Check browser dev tools → Application → Cookies

### Session expired too quickly

- Adjust `SESSION_TIMEOUT_MINUTES` environment variable
- Check Redis maxmemory policy

## Architecture Notes

- Sessions are sticky-free: any backend instance can handle any request
- Session data stored as serialized Java objects in Redis
- Session IDs are cryptographically secure random values
- No session replication needed (single source of truth in Redis)
