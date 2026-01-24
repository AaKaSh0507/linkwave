# Linkwave Docker Development Environment

Complete local development environment setup for the Linkwave chat application using Docker Compose.

## Services Provisioned

| Service | Container Name | Image | Port(s) | Purpose |
|---------|---------------|-------|---------|---------|
| **postgres-db** | linkwave-postgres | postgres:17.2-alpine | 5432 | Primary database for REST resources |
| **redis-cache** | linkwave-redis | redis:7.4.2-alpine | 6379 | Session storage & presence tracking |
| **kafka-broker** | linkwave-kafka | confluentinc/cp-kafka:7.8.0 | 9092, 29092 | Event streaming for chat messages |
| **kafka-zookeeper** | linkwave-zookeeper | confluentinc/cp-zookeeper:7.8.0 | 2181 | Kafka coordination service |
| **smtp-mailhog** | linkwave-mailhog | mailhog/mailhog:v1.0.1 | 1025, 8025 | SMTP mock server for email testing |

## Prerequisites

- Docker Desktop 4.x or later
- Docker Compose v2.x or later
- At least 4GB RAM allocated to Docker
- Ports 5432, 6379, 2181, 9092, 29092, 1025, 8025 available

## Quick Start

### 1. Environment Configuration

Copy the example environment file and update credentials:

```bash
cp .env.example .env
```

Edit `.env` with your desired credentials:
```bash
# PostgreSQL
POSTGRES_DB=linkwave
POSTGRES_USER=linkwave
POSTGRES_PASSWORD=your_secure_password

# Redis
REDIS_PASSWORD=your_redis_password

# Mailhog (ports are configurable)
MAILHOG_SMTP_PORT=1025
MAILHOG_UI_PORT=8025
```

**Security Note**: Never commit `.env` to version control. It's already in `.gitignore`.

### 2. Start All Services

```bash
docker compose up -d
```

This will:
- Create a shared network `linkwave-network`
- Pull all required images
- Start all services in detached mode
- Create persistent volumes for data

### 3. Verify Services

Check all services are running:

```bash
docker compose ps
```

Expected output:
```
NAME                 IMAGE                             STATUS
linkwave-postgres    postgres:17.2-alpine              Up (healthy)
linkwave-redis       redis:7.4.2-alpine                Up (healthy)
linkwave-kafka       confluentinc/cp-kafka:7.8.0       Up (healthy)
linkwave-zookeeper   confluentinc/cp-zookeeper:7.8.0   Up (healthy)
linkwave-mailhog     mailhog/mailhog:v1.0.1            Up (healthy)
```

## Service Details

### PostgreSQL Database (postgres-db)

**Connection String**:
```
jdbc:postgresql://localhost:5432/linkwave
```

**Credentials** (from `.env`):
- Database: `linkwave`
- User: `linkwave`
- Password: (set in `.env`)

**Data Persistence**:
- Volume: `linkwave-postgres-data`
- Path: `/var/lib/postgresql/data`
- Data persists across container restarts

**Connect via CLI**:
```bash
docker exec -it linkwave-postgres psql -U linkwave -d linkwave
```

**Healthcheck**: Checks `pg_isready` every 10 seconds

### Redis Cache (redis-cache)

**Connection**:
```
Host: localhost
Port: 6379
Password: (set in .env as REDIS_PASSWORD)
```

**Data Persistence**:
- Volume: `linkwave-redis-data`
- Path: `/data`
- RDB snapshots enabled

**Connect via CLI**:
```bash
docker exec -it linkwave-redis redis-cli
# Then authenticate
AUTH your_redis_password
```

**Test Connection**:
```bash
docker exec -it linkwave-redis redis-cli -a your_redis_password ping
# Expected output: PONG
```

**Healthcheck**: Increments ping counter every 10 seconds

### Kafka Broker (kafka-broker)

**Connection**:
- Internal (from Docker): `kafka-broker:9092`
- External (from host): `localhost:29092`

**Topics**:
- Auto-creation enabled
- Replication factor: 1 (single broker)

**Zookeeper Connection**:
- Managed by `kafka-zookeeper` service
- Port: 2181

**List Topics**:
```bash
docker exec -it linkwave-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

**Create Test Topic**:
```bash
docker exec -it linkwave-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --create \
  --topic test-messages \
  --partitions 1 \
  --replication-factor 1
```

**Healthcheck**: Verifies broker API every 15 seconds (30s start period)

### Mailhog SMTP Server (smtp-mailhog)

**SMTP Server**:
```
Host: localhost
Port: 1025
No authentication required
```

**Web UI**:
```
http://localhost:8025
```

**Features**:
- Captures all emails sent to port 1025
- View emails in web interface
- No emails are actually delivered (perfect for testing)
- Emails cleared on container restart

**Test Email**:
```bash
# Using telnet
telnet localhost 1025
EHLO localhost
MAIL FROM: test@linkwave.com
RCPT TO: user@linkwave.com
DATA
Subject: Test Email
This is a test email from Linkwave
.
QUIT
```

Then view at http://localhost:8025

**Healthcheck**: HTTP check on web UI every 10 seconds

## Common Operations

### View Logs

All services:
```bash
docker compose logs -f
```

Specific service:
```bash
docker compose logs -f postgres-db
docker compose logs -f redis-cache
docker compose logs -f kafka-broker
docker compose logs -f smtp-mailhog
```

### Stop Services

```bash
docker compose stop
```

Services stop but containers and volumes remain.

### Start Stopped Services

```bash
docker compose start
```

### Restart Services

```bash
docker compose restart
```

Or specific service:
```bash
docker compose restart postgres-db
```

### Stop and Remove Containers

```bash
docker compose down
```

**Warning**: This removes containers but preserves volumes (data persists).

### Stop and Remove Everything (Including Data)

```bash
docker compose down -v
```

**Danger**: This removes all volumes. All data will be lost!

### Rebuild Services

```bash
docker compose up -d --build
```

## Healthcheck Verification

Check detailed health status:

```bash
docker inspect linkwave-postgres --format='{{.State.Health.Status}}'
docker inspect linkwave-redis --format='{{.State.Health.Status}}'
docker inspect linkwave-kafka --format='{{.State.Health.Status}}'
docker inspect linkwave-zookeeper --format='{{.State.Health.Status}}'
docker inspect linkwave-mailhog --format='{{.State.Health.Status}}'
```

All should return `healthy` after services fully start.

### Healthcheck Intervals

| Service | Interval | Timeout | Retries | Start Period |
|---------|----------|---------|---------|--------------|
| postgres-db | 10s | 5s | 5 | 10s |
| redis-cache | 10s | 5s | 5 | 5s |
| kafka-zookeeper | 10s | 5s | 5 | 10s |
| kafka-broker | 15s | 10s | 5 | 30s |
| smtp-mailhog | 10s | 5s | 5 | 5s |

## Network Configuration

All services run on a shared user-defined bridge network:

**Network Name**: `linkwave-network`

**DNS Resolution**: Services can reach each other by name:
- `postgres-db:5432`
- `redis-cache:6379`
- `kafka-broker:9092`
- `kafka-zookeeper:2181`
- `smtp-mailhog:1025`

**Inspect Network**:
```bash
docker network inspect linkwave-network
```

## Volume Management

### List Volumes

```bash
docker volume ls | grep linkwave
```

Expected volumes:
- `linkwave-postgres-data` - PostgreSQL database files
- `linkwave-redis-data` - Redis persistence files
- `linkwave-kafka-data` - Kafka message logs
- `linkwave-zookeeper-data` - Zookeeper data
- `linkwave-zookeeper-logs` - Zookeeper transaction logs

### Inspect Volume

```bash
docker volume inspect linkwave-postgres-data
```

### Backup PostgreSQL Data

```bash
docker exec linkwave-postgres pg_dump -U linkwave linkwave > backup.sql
```

### Restore PostgreSQL Data

```bash
docker exec -i linkwave-postgres psql -U linkwave -d linkwave < backup.sql
```

### Remove Specific Volume (Danger!)

```bash
docker volume rm linkwave-postgres-data
```

**Warning**: This permanently deletes all data in that volume!

## Troubleshooting

### Services Not Starting

1. Check Docker is running:
   ```bash
   docker ps
   ```

2. Check port conflicts:
   ```bash
   lsof -i :5432  # PostgreSQL
   lsof -i :6379  # Redis
   lsof -i :9092  # Kafka
   lsof -i :8025  # Mailhog UI
   ```

3. View service logs:
   ```bash
   docker compose logs postgres-db
   ```

### Kafka Not Ready

Kafka takes longer to start (up to 30 seconds). Check logs:

```bash
docker compose logs kafka-broker
```

Wait for: `[KafkaServer id=1] started`

### Redis Authentication Fails

Ensure `REDIS_PASSWORD` in `.env` matches what you're using to connect.

### PostgreSQL Connection Refused

1. Ensure service is healthy:
   ```bash
   docker compose ps postgres-db
   ```

2. Check credentials match `.env` file

3. Verify port mapping:
   ```bash
   docker port linkwave-postgres
   ```

### Cannot Access Mailhog UI

1. Verify service is running:
   ```bash
   docker compose ps smtp-mailhog
   ```

2. Check if port 8025 is in use:
   ```bash
   lsof -i :8025
   ```

3. Try alternate port in `.env`:
   ```env
   MAILHOG_UI_PORT=8026
   ```
   Then restart: `docker compose up -d smtp-mailhog`

## Data Persistence Notes

### What Persists

✅ **PostgreSQL**: All database tables, schemas, and data  
✅ **Redis**: RDB snapshots (periodic saves)  
✅ **Kafka**: Message logs and topics  
✅ **Zookeeper**: Cluster metadata  

### What Doesn't Persist

❌ **Mailhog**: Emails are not persisted (cleared on restart)  
❌ **Redis**: In-memory data between snapshots (if crash occurs)  

### Clean Slate Reset

To start completely fresh with no data:

```bash
docker compose down -v
docker compose up -d
```

This is useful for:
- Testing migrations
- Resetting test data
- Troubleshooting data corruption

## Integration with Backend

**Note**: The backend Spring Boot application is NOT configured to use these services yet. This is environment scaffolding only.

To connect the backend later, update `backend/src/main/resources/application.yml`:

```yaml
# Future configuration (not implemented yet)
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/linkwave
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}
  
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD}
  
  kafka:
    bootstrap-servers: localhost:29092
  
  mail:
    host: localhost
    port: 1025
```

## Production Considerations

This setup is for **local development only**. For production:

- ❌ Do not use default passwords
- ❌ Do not expose all ports publicly
- ❌ Do not use single Kafka broker
- ✅ Use managed database services
- ✅ Implement proper authentication
- ✅ Use TLS/SSL encryption
- ✅ Configure Redis persistence properly
- ✅ Set up Kafka clusters with replication
- ✅ Use production-grade email service

## Version Information

| Component | Version |
|-----------|---------|
| PostgreSQL | 17.2 |
| Redis | 7.4.2 |
| Kafka | 7.8.0 (Confluent Platform) |
| Zookeeper | 7.8.0 (Confluent Platform) |
| Mailhog | 1.0.1 |

## Additional Resources

- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Redis Documentation](https://redis.io/docs/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Mailhog Documentation](https://github.com/mailhog/MailHog)
- [Docker Compose Documentation](https://docs.docker.com/compose/)

## Support

For issues with:
- **Services not starting**: Check logs with `docker compose logs`
- **Port conflicts**: Update ports in `.env` file
- **Data persistence**: See Volume Management section
- **Connection issues**: See Troubleshooting section
