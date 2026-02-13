# Linkwave Application Metrics

## Overview

Linkwave uses **Micrometer + Prometheus** for metrics collection and **Grafana** for visualization.

| Port | Service          | URL                                               |
|------|------------------|----------------------------------------------------|
| 8080 | Actuator Metrics | `http://localhost:8080/actuator/prometheus`         |
| 9090 | Prometheus UI    | `http://localhost:9090`                             |
| 3001 | Grafana UI       | `http://localhost:3001` (admin/admin)               |

## Custom Metrics

### WebSocket

| Metric                                   | Type    | Labels                                      | Description                           |
|------------------------------------------|---------|----------------------------------------------|---------------------------------------|
| `websocket.connections.active`           | Gauge   | —                                            | Current active WebSocket connections  |
| `websocket.connections.total`            | Counter | `event=connected\|disconnected`              | Total connection events               |
| `websocket.messages.total`              | Counter | `direction=inbound\|outbound`                | Total messages sent/received          |
| `websocket.message.processing.duration` | Timer   | —                                            | Message processing latency            |
| `websocket.errors.total`                | Counter | `type=connection\|message\|authentication`   | Error counts by type                  |

### Chat & Messaging

| Metric                             | Type    | Labels                     | Description                    |
|------------------------------------|---------|----------------------------|--------------------------------|
| `messages.sent.total`              | Counter | `status=success\|failure`  | Messages published to Kafka    |
| `messages.persistence.duration`    | Timer   | —                          | DB persistence latency         |
| `messages.size.bytes`              | Summary | —                          | Message body size distribution |

### Kafka

| Metric                            | Type    | Labels                  | Description              |
|-----------------------------------|---------|-------------------------|--------------------------|
| `kafka.messages.produced.total`   | Counter | `topic=chat.messages`   | Messages produced        |
| `kafka.messages.consumed.total`   | Counter | `topic=chat.messages`   | Messages consumed        |
| `kafka.consume.duration`          | Timer   | —                       | Consumer processing time |
| `kafka.errors.total`              | Counter | `operation=consume`     | Consumer errors          |

### OTP / Authentication

| Metric                      | Type    | Labels                               | Description               |
|-----------------------------|---------|---------------------------------------|---------------------------|
| `otp.requests.total`        | Counter | `status=success\|failure`            | OTP generation requests   |
| `otp.verifications.total`   | Counter | `result=success\|failure\|expired`   | OTP verification attempts |
| `otp.generation.duration`   | Timer   | —                                     | OTP generation latency    |
| `otp.active.count`          | Gauge   | —                                     | Currently active OTPs     |

### Presence

| Metric                          | Type    | Labels                   | Description                |
|---------------------------------|---------|--------------------------|----------------------------|
| `presence.updates.total`        | Counter | `status=online\|offline` | Presence state changes     |
| `presence.heartbeat.duration`   | Timer   | —                        | Heartbeat processing time  |

### Typing

| Metric                  | Type    | Labels               | Description              |
|-------------------------|---------|----------------------|--------------------------|
| `typing.events.total`  | Counter | `action=start\|stop` | Typing indicator events  |
| `typing.users.active`  | Gauge   | —                    | Currently typing users   |

### Retention

| Metric                                | Type    | Labels                    | Description            |
|---------------------------------------|---------|---------------------------|------------------------|
| `retention.job.executions.total`      | Counter | `status=success\|failure` | Retention job runs     |
| `retention.messages.deleted.total`    | Counter | —                         | Messages cleaned up    |
| `retention.job.duration`              | Timer   | —                         | Job execution time     |

## Default Metrics (Auto-collected)

- **JVM**: Memory, GC, threads, classloader
- **System**: CPU usage, file descriptors
- **HTTP**: Request count, latency by method/URI/status
- **Tomcat**: Active sessions, threads
- **HikariCP**: Connection pool metrics
- **Logback**: Log events by level

## Alert Rules

| Alert                        | Condition                          | Severity |
|------------------------------|------------------------------------|----------|
| `HighErrorRate`              | WS errors > 1/sec for 2m          | Warning  |
| `HighLatency`                | WS p95 > 500ms for 5m             | Warning  |
| `WebSocketConnectionsDrop`   | Connections drop > 10 in 5m        | Critical |
| `KafkaConsumerErrors`        | Consumer errors > 0.1/sec for 5m   | Warning  |
| `RetentionJobFailure`        | Any failure in 1h                  | Critical |

## Example PromQL Queries

```promql
# Active WebSocket connections
websocket_connections_active

# Message send rate (success vs failure)
rate(messages_sent_total{status="success"}[5m])
rate(messages_sent_total{status="failure"}[5m])

# p95 WebSocket message processing latency
histogram_quantile(0.95, rate(websocket_message_processing_duration_seconds_bucket[5m]))

# OTP verification success rate
rate(otp_verifications_total{result="success"}[5m]) / rate(otp_verifications_total[5m])
```
