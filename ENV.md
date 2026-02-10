# LinkWave Environment Variables Documentation

## Overview

This document provides comprehensive documentation for all environment variables used in the LinkWave application. Variables are organized by service and category.

---

## Database (PostgreSQL)

### `POSTGRES_DB`
- **Description**: PostgreSQL database name
- **Required**: Yes
- **Default**: `linkwave`
- **Example**: `linkwave_production`
- **Environment**: All

### `POSTGRES_USER`
- **Description**: PostgreSQL database user
- **Required**: Yes
- **Default**: `linkwave`
- **Example**: `linkwave_app`
- **Environment**: All

### `POSTGRES_PASSWORD`
- **Description**: PostgreSQL database password
- **Required**: Yes
- **Default**: None (must be set)
- **Example**: `your_secure_password_here`
- **Environment**: All
- **Security**: ⚠️ **SENSITIVE** - Never commit to Git

### `POSTGRES_PORT`
- **Description**: PostgreSQL port for external access
- **Required**: No
- **Default**: `5432` (dev: `5433`)
- **Example**: `5432`
- **Environment**: Dev, Local

### `DATABASE_URL`
- **Description**: Full JDBC connection URL for Spring Boot
- **Required**: Yes (via environment) 
- **Default**: `jdbc:postgresql://localhost:5432/linkwave`
- **Example**: `jdbc:postgresql://postgres:5432/linkwave_production`
- **Environment**: All

### `DATABASE_USERNAME`
- **Description**: Application database username (same as POSTGRES_USER)
- **Required**: Yes
- **Default**: `linkwave`
- **Example**: `linkwave_app`
- **Environment**: All

### `DATABASE_PASSWORD`
- **Description**: Application database password (same as POSTGRES_PASSWORD)
- **Required**: Yes
- **Default**: None
- **Example**: `your_secure_password_here`
- **Environment**: All
- **Security**: ⚠️ **SENSITIVE** - Never commit to Git

---

## Redis Cache

### `REDIS_HOST`
- **Description**: Redis server hostname
- **Required**: Yes
- **Default**: `localhost`
- **Example**: `redis` (Docker), `redis.example.com` (Cloud)
- **Environment**: All

### `REDIS_PORT`
- **Description**: Redis server port
- **Required**: No
- **Default**: `6379` (dev: `6380`)
- **Example**: `6379`
- **Environment**: All

### `REDIS_PASSWORD`
- **Description**: Redis authentication password
- **Required**: Yes (production)
- **Default**: Empty (dev only)
- **Example**: `your_secure_redis_password`
- **Environment**: All
- **Security**: ⚠️ **SENSITIVE** - Required for production

---

## Kafka Message Broker

### `KAFKA_BOOTSTRAP_SERVERS`
- **Description**: Kafka broker addresses (comma-separated)
- **Required**: Yes
- **Default**: `localhost:9092`
- **Example**: `kafka:9092` (Docker), `broker1:9092,broker2:9092` (Cluster)
- **Environment**: All

### `KAFKA_CONSUMER_GROUP_ID`
- **Description**: Kafka consumer group identifier
- **Required**: No
- **Default**: `linkwave-chat-delivery`
- **Example**: `linkwave-chat-delivery-prod`
- **Environment**: All

---

## Email / SMTP

### `MAIL_HOST`
- **Description**: SMTP server hostname
- **Required**: Yes (production)
- **Default**: Empty (dev: `localhost` for Mailhog)
- **Example**: `smtp.gmail.com`, `smtp.sendgrid.net`
- **Environment**: Production

### `MAIL_PORT`
- **Description**: SMTP server port
- **Required**: No
- **Default**: `587` (TLS), `1025` (dev: Mailhog)
- **Example**: `587`, `465` (SSL)
- **Environment**: All

### `MAIL_USERNAME`
- **Description**: SMTP authentication username
- **Required**: Yes (if SMTP auth required)
- **Default**: Empty
- **Example**: `your_email@example.com`
- **Environment**: Production
- **Security**: ⚠️ **SENSITIVE**

### `MAIL_PASSWORD`
- **Description**: SMTP authentication password
- **Required**: Yes (if SMTP auth required)
- **Default**: Empty
- **Example**: `your_app_specific_password`
- **Environment**: Production
- **Security**: ⚠️ **SENSITIVE** - Never commit to Git

### `MAIL_FROM`
- **Description**: Default "From" email address
- **Required**: No
- **Default**: `no-reply@linkwave.app`
- **Example**: `noreply@yourdomain.com`
- **Environment**: All

### `MAIL_TLS_ENABLED`
- **Description**: Enable STARTTLS for SMTP
- **Required**: No
- **Default**: `true` (prod), `false` (dev)
- **Example**: `true`
- **Environment**: All

---

## Application Server

### `SERVER_PORT`
- **Description**: Spring Boot application port
- **Required**: No
- **Default**: `8080`
- **Example**: `8080`, `8443`
- **Environment**: All

### `API_PORT`
- **Description**: External API port mapping (Docker)
- **Required**: No
- **Default**: `8080`
- **Example**: `8080`
- **Environment**: Dev, Local

### `SPRING_PROFILES_ACTIVE`
- **Description**: Active Spring profile
- **Required**: No
- **Default**: `local`
- **Example**: `local`, `prod`
- **Environment**: All
- **Valid Values**: `local`, `prod`

---

## Frontend

### `FRONTEND_PORT`
- **Description**: Next.js frontend port (Docker)
- **Required**: No
- **Default**: `3000`
- **Example**: `3000`
- **Environment**: Dev, Local

### `PUBLIC_API_URL`
- **Description**: Public-facing API URL (for Docker build args)
- **Required**: Yes (production)
- **Default**: `http://localhost:8080`
- **Example**: `https://api.yourdomain.com`
- **Environment**: Production

### `PUBLIC_WS_URL`
- **Description**: Public-facing WebSocket URL (for Docker build args)
- **Required**: Yes (production)
- **Default**: `ws://localhost:8080/ws`
- **Example**: `wss://api.yourdomain.com/ws`
- **Environment**: Production

### `NEXT_PUBLIC_API_URL`
- **Description**: API URL for frontend (runtime)
- **Required**: Yes
- **Default**: `http://localhost:8080/api/v1`
- **Example**: `https://api.yourdomain.com/api/v1`
- **Environment**: All
- **Note**: Must include `/api/v1` path

### `NEXT_PUBLIC_WS_URL`
- **Description**: WebSocket URL for frontend (runtime)
- **Required**: Yes
- **Default**: `localhost:8080`
- **Example**: `api.yourdomain.com`
- **Environment**: All
- **Note**: Do not include `ws://` or `wss://` prefix

### `NEXT_PUBLIC_API_TIMEOUT`
- **Description**: API request timeout in milliseconds
- **Required**: No
- **Default**: `30000` (30 seconds)
- **Example**: `30000`
- **Environment**: All

### `NEXT_PUBLIC_WS_RECONNECT_DELAY`
- **Description**: WebSocket reconnection delay in milliseconds
- **Required**: No
- **Default**: `3000`
- **Example**: `3000`
- **Environment**: All

### `NEXT_PUBLIC_WS_MAX_RECONNECT`
- **Description**: Maximum WebSocket reconnection attempts
- **Required**: No
- **Default**: `5`
- **Example**: `5`
- **Environment**: All

---

## Session & Security

### `SESSION_TIMEOUT`
- **Description**: Session timeout duration
- **Required**: No
- **Default**: `30m` (dev), `60m` (prod)
- **Example**: `30m`, `1h`, `3600s`
- **Environment**: All
- **Format**: Duration (s, m, h)

### `SESSION_NAMESPACE`
- **Description**: Redis key prefix for sessions
- **Required**: No
- **Default**: `linkwave:session:`
- **Example**: `linkwave:prod:session:`
- **Environment**: All

### `SESSION_TIMEOUT_MINUTES`
- **Description**: Session timeout in minutes (legacy)
- **Required**: No
- **Default**: `30`
- **Example**: `60`
- **Environment**: All

### `COOKIE_SECURE`
- **Description**: Enable secure flag on cookies (HTTPS only)
- **Required**: No
- **Default**: `false` (dev), `true` (prod)
- **Example**: `true`
- **Environment**: All
- **Production**: ⚠️ **Must be `true`** for HTTPS

### `COOKIE_SAME_SITE`
- **Description**: SameSite cookie attribute
- **Required**: No
- **Default**: `lax` (dev), `strict` (prod)
- **Example**: `strict`, `lax`, `none`
- **Environment**: All
- **Valid Values**: `strict`, `lax`, `none`

### `COOKIE_DOMAIN`
- **Description**: Cookie domain scope
- **Required**: No
- **Default**: Empty (current domain)
- **Example**: `.yourdomain.com`
- **Environment**: Production

---

## OTP (One-Time Password)

### `OTP_LENGTH`
- **Description**: Length of generated OTP codes
- **Required**: No
- **Default**: `6`
- **Example**: `6`, `8`
- **Environment**: All

### `OTP_TTL_SECONDS`
- **Description**: OTP validity period in seconds
- **Required**: No
- **Default**: `300` (5 minutes)
- **Example**: `300`, `600`
- **Environment**: All

### `OTP_THROTTLE_MAX_REQUESTS`
- **Description**: Maximum OTP requests within throttle window
- **Required**: No
- **Default**: `3`
- **Example**: `3`, `5`
- **Environment**: All

### `OTP_THROTTLE_WINDOW_SECONDS`
- **Description**: Throttle window duration in seconds
- **Required**: No
- **Default**: `600` (10 minutes)
- **Example**: `600`, `900`
- **Environment**: All

---

## Data Retention

### `RETENTION_DAYS`
- **Description**: Message retention period in days
- **Required**: No
- **Default**: `7`
- **Example**: `7`, `30`, `90`
- **Environment**: All

### `RETENTION_BATCH_SIZE`
- **Description**: Batch size for deletion operations
- **Required**: No
- **Default**: `1000`
- **Example**: `1000`, `5000`
- **Environment**: All

### `RETENTION_CRON`
- **Description**: Cron expression for retention job
- **Required**: No
- **Default**: `0 0 2 ? * SUN` (Every Sunday at 2 AM)
- **Example**: `0 0 3 * * *` (Daily at 3 AM)
- **Environment**: All
- **Format**: Cron expression

### `RETENTION_MAX_RETRIES`
- **Description**: Maximum retry attempts for failed deletions
- **Required**: No
- **Default**: `3`
- **Example**: `3`, `5`
- **Environment**: All

### `RETENTION_RETRY_DELAY_MS`
- **Description**: Delay between retry attempts in milliseconds
- **Required**: No
- **Default**: `1000`
- **Example**: `1000`, `5000`
- **Environment**: All

---

## Feature Flags (Frontend)

### `NEXT_PUBLIC_DEBUG_LOGGING`
- **Description**: Enable debug logging in browser console
- **Required**: No
- **Default**: `true` (dev), `false` (prod)
- **Example**: `true`
- **Environment**: Dev, Local

### `NEXT_PUBLIC_TYPING_INDICATORS`
- **Description**: Enable typing indicator feature
- **Required**: No
- **Default**: `true`
- **Example**: `true`
- **Environment**: All

### `NEXT_PUBLIC_READ_RECEIPTS`
- **Description**: Enable read receipt feature
- **Required**: No
- **Default**: `true`
- **Example**: `true`
- **Environment**: All

### `NEXT_PUBLIC_PRESENCE`
- **Description**: Enable presence (online/offline) feature
- **Required**: No
- **Default**: `true`
- **Example**: `true`
- **Environment**: All

### `NEXT_PUBLIC_MAX_MESSAGE_LENGTH`
- **Description**: Maximum message length in characters
- **Required**: No
- **Default**: `5000`
- **Example**: `5000`, `10000`
- **Environment**: All

### `NEXT_PUBLIC_MESSAGE_RETENTION_DAYS`
- **Description**: Display value for message retention
- **Required**: No
- **Default**: `7`
- **Example**: `7`, `30`
- **Environment**: All
- **Note**: Should match backend `RETENTION_DAYS`

### `NEXT_PUBLIC_MESSAGE_PAGE_SIZE`
- **Description**: Number of messages loaded per page
- **Required**: No
- **Default**: `50`
- **Example**: `50`, `100`
- **Environment**: All

---

## Logging

### `LOG_FILE`
- **Description**: Log file path
- **Required**: No
- **Default**: `logs/linkwave.log`
- **Example**: `/var/log/linkwave/app.log`
- **Environment**: All

---

## Quick Setup Guide

### Development (.env.local)
```bash
# Database
POSTGRES_PASSWORD=dev_password

# Redis
REDIS_PASSWORD=dev_redis_pass

# Email (Mailhog)
MAIL_HOST=localhost
MAIL_PORT=1025
MAIL_TLS_ENABLED=false
```

### Production (.env)
```bash
# Database (Required)
POSTGRES_DB=linkwave_prod
POSTGRES_USER=linkwave_prod
POSTGRES_PASSWORD=<strong_password>

# Redis (Required)
REDIS_PASSWORD=<strong_redis_password>

# Email (Required)
MAIL_HOST=smtp.sendgrid.net
MAIL_PORT=587
MAIL_USERNAME=apikey
MAIL_PASSWORD=<sendgrid_api_key>
MAIL_FROM=noreply@yourdomain.com

# Public URLs (Required)
PUBLIC_API_URL=https://api.yourdomain.com
PUBLIC_WS_URL=wss://api.yourdomain.com/ws

# Security (Required)
COOKIE_SECURE=true
COOKIE_SAME_SITE=strict
SESSION_TIMEOUT=60m
```

---

## Environment File Locations

- **Root**: `.env` (Docker Compose variables)
- **Root**: `.env.example` (Template with documentation)
- **Frontend**: `frontend/.env.local` (Development overrides)
- **Backend**: Uses environment variables from Docker/system

## Security Best Practices

1. ⚠️ **Never commit .env files to Git** (.gitignore includes them)
2. ⚠️ **Use strong, unique passwords** for all services
3. ⚠️ **Enable TLS/SSL** in production (`MAIL_TLS_ENABLED`, `COOKIE_SECURE`)
4. ⚠️ **Rotate credentials regularly** (database, Redis, SMTP)
5. ⚠️ **Use environment-specific values** (don't reuse dev passwords in prod)
6. ⚠️ **Monitor sensitive variables** in logs and error messages

---

## Environment Profiles

### `local` (Default Development)
- Mailhog for email testing
- Relaxed security (HTTP cookies)
- Debug logging enabled
- PostgreSQL on port 5433
- Redis on port 6380

### `prod` (Production)
- Real SMTP server required
- Strict security (HTTPS cookies)
- INFO level logging
- Swagger/Actuator disabled
- Longer session timeout (60m)

---

## Validation

Run this command to validate required environment variables are set:

```bash
# Development
docker-compose config

# Production
docker-compose -f docker-compose.prod.yml config
```

---

**Last Updated**: Phase 2 - Configuration Review  
**Maintainer**: LinkWave Team
