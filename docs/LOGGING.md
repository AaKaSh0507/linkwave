# Logging Guide

## Overview

Linkwave uses structured logging across backend (SLF4J + Logback) and frontend (custom `logger.ts` utility).

## Backend

### Profiles

| Profile                   | Output          | Root Level | App Level |
|---------------------------|-----------------|------------|-----------|
| `default`, `local`, `dev` | Colored console | INFO       | DEBUG     |
| `prod`, `production`      | JSON (Logstash) | WARN       | INFO      |

### MDC Fields (auto-injected)

| Field            | Source                | Description                                                 |
|------------------|-----------------------|-------------------------------------------------------------|
| `correlation_id` | `CorrelationIdFilter` | UUID per request, propagated via `X-Correlation-ID` header  |
| `user_id`        | `UserContextFilter`   | Masked phone (last 4 digits) from SecurityContext           |

### Request Timing

`RequestTimingFilter` logs every HTTP request with `method`, `uri`, `status`, `duration_ms`. Requests >1s are logged at WARN. Actuator and WebSocket endpoints are excluded.

### Log Levels

- **ERROR**: Unrecoverable failures (email delivery, transport errors)
- **WARN**: Security events (OTP failures, throttling, auth violations), slow requests
- **INFO**: Business events (OTP requested/verified, session created, read receipts)
- **DEBUG**: Protocol-level detail (WS messages, heartbeats, typing events)

### Sensitive Data

- Phone numbers: masked to `***XXXX` (last 4 digits)
- Emails: masked to `XX***@domain.com`
- OTP codes: **never** logged in production (dev mode only)

### Example: JSON log (prod)

```json
{
  "@timestamp": "2026-02-13T12:30:45.123Z",
  "level": "INFO",
  "logger_name": "c.l.a.s.auth.OtpService",
  "message": "OTP requested: phone=***1234",
  "correlation_id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "user_id": "***1234",
  "app": "linkwave",
  "env": "production"
}
```

### Querying with jq

```bash
# All errors
cat app.log | jq 'select(.level == "ERROR")'

# Trace a request
cat app.log | jq 'select(.correlation_id == "a1b2c3d4-...")'

# Slow requests
cat app.log | jq 'select(.message | contains("Slow request"))'
```

## Frontend

### Logger (`lib/logger.ts`)

```typescript
import { logger } from '@/lib/logger'

logger.info('User connected', 'chat', { userId: '123' })
logger.error('Failed to send', 'api', { status: 500 })
logger.warn('Rate limited', 'websocket')
logger.debug('Heartbeat sent', 'websocket')  // suppressed in production
```

### Output

- **Development**: Human-readable with timestamp, level, context, message
- **Production**: JSON (`{ level, message, timestamp, context, ...meta }`)

### ESLint

Direct `console.*` usage is blocked by `"no-console": "error"`. Use `// eslint-disable-next-line no-console` only inside `logger.ts` itself.
