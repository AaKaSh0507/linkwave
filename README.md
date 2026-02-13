# Linkwave

[![Build and Push](https://github.com/AaKaSh0507/linkwave/actions/workflows/build-and-push.yml/badge.svg)](https://github.com/AaKaSh0507/linkwave/actions/workflows/build-and-push.yml)
[![Pre-commit](https://github.com/AaKaSh0507/linkwave/actions/workflows/pre-commit.yml/badge.svg)](https://github.com/AaKaSh0507/linkwave/actions/workflows/pre-commit.yml)

Real-time chat application with WebSocket messaging, presence indicators, typing indicators, and read receipts.

## Architecture

```text
┌─────────────┐     ┌──────────────┐     ┌───────────┐
│  Next.js    │────▶│  Spring Boot │────▶│ PostgreSQL│
│  Frontend   │     │  Backend     │     └───────────┘
└─────────────┘     │              │     ┌───────────┐
     WebSocket ────▶│  /ws         │────▶│   Redis   │
                    │              │     └───────────┘
                    │              │     ┌───────────┐
                    │  Kafka       │────▶│   Kafka   │
                    └──────────────┘     └───────────┘
```

| Component | Stack |
|-----------|-------|
| Frontend | Next.js 15, React 19, TypeScript, TailwindCSS, ShadCN |
| Backend | Spring Boot 3.x, Java 21, WebSocket, JPA |
| Database | PostgreSQL 16 |
| Cache | Redis 7 — sessions, presence, typing state, pub/sub |
| Messaging | Apache Kafka — chat message delivery |
| Monitoring | Prometheus + Grafana |
| Proxy | Nginx (Docker) / Traefik (Kubernetes) |

---

## Quick Start

### Prerequisites

- Docker & Docker Compose
- Java 21, Node.js 20+ (for local dev without Docker)

### Run Everything

```bash
git clone https://github.com/AaKaSh0507/linkwave.git
cd linkwave
cp config/env/.env.example .env
make dev
```

| Service | URL |
|---------|-----|
| Frontend | <http://localhost:3000> |
| Backend API | <http://localhost:8080> |
| WebSocket | ws://localhost:8080/ws |
| MailHog | <http://localhost:8025> |
| Prometheus | <http://localhost:9090> |
| Grafana | <http://localhost:3001> (admin/admin) |

### Stop

```bash
make down
```

### Run Without Docker

```bash
# Backend
cd backend && ./gradlew bootRun

# Frontend
cd frontend && npm install && npm run dev
```

---

## Project Structure

```text
linkwave/
├── backend/              # Spring Boot API
│   ├── src/main/java/    #   config, controller, domain, exception,
│   │                     #   kafka, repository, security, service, websocket
│   └── src/main/resources/
│       ├── application.yml
│       ├── application-local.yml
│       ├── logback-spring.xml
│       └── db/migration/
├── frontend/             # Next.js client
│   ├── app/              #   Pages and layouts
│   ├── components/       #   auth/, chat/, ui/
│   └── lib/              #   api/, hooks/, contexts, websocket
├── config/               # Project-level configuration
│   ├── hooks/            #   Custom pre-commit hooks
│   ├── env/              #   .env.example template
│   ├── .editorconfig
│   ├── commitlint.config.js
│   ├── .markdownlint.json
│   ├── .gitleaks.toml
│   └── .secrets.baseline
├── deployments/          # Docker Compose files
│   ├── docker-compose.dev.yml
│   └── docker-compose.prod.yml
├── docker/               # Dockerfiles
│   ├── api/
│   ├── frontend/
│   ├── db/
│   └── nginx/
├── k8s/                  # Kubernetes manifests
│   ├── base/             #   namespace, secrets, configmap
│   ├── backend/          #   API deployment + service
│   ├── frontend/         #   Frontend deployment + service
│   ├── postgres/         #   PostgreSQL statefulset
│   ├── redis/            #   Redis deployment
│   ├── kafka/            #   Kafka + Zookeeper
│   ├── ingress/          #   Traefik, TLS, middleware
│   ├── prometheus/       #   Prometheus deployment
│   └── grafana/          #   Grafana deployment
├── prometheus/           # Prometheus config + alerts
├── grafana/              # Dashboards + provisioning
├── scripts/              # Utility scripts
│   ├── dev-up.sh / dev-down.sh
│   ├── prod-up.sh / prod-down.sh
│   ├── k8s-deploy.sh / k8s-rollback.sh
│   ├── install-cert-manager.sh
│   └── setup-pre-commit.sh
├── .github/workflows/    # CI/CD
├── .husky/               # Git hooks
├── Makefile
├── docker-compose.yml    # → deployments/docker-compose.dev.yml
└── CHANGELOG.md
```

---

## Make Targets

```bash
make help             # Show all commands
make dev              # Start dev environment
make down             # Stop services
make restart          # Restart all services
make logs             # Tail all logs
make logs-api         # Tail API logs only
make health           # Check service health
make test             # Run backend tests
make lint             # Run all linters (pre-commit)
make prod             # Start production environment
make prod-down        # Stop production
make clean            # Remove containers
make clean-all        # Remove containers, volumes, images
make setup            # Initial setup (env, build, hooks)
make k8s-deploy       # Deploy to Kubernetes
make k8s-rollback     # Rollback Kubernetes deployments
```

---

## Environment Variables

Copy `config/env/.env.example` to `.env` and configure:

| Variable | Default | Description |
|----------|---------|-------------|
| `POSTGRES_DB` | `linkwave` | Database name |
| `POSTGRES_USER` | `linkwave` | Database user |
| `POSTGRES_PASSWORD` | — | Database password (**required**) |
| `REDIS_PASSWORD` | — | Redis password |
| `KAFKA_BOOTSTRAP_SERVERS` | `kafka:9092` | Kafka broker address |
| `API_PORT` | `8080` | Backend port |
| `FRONTEND_PORT` | `3000` | Frontend port |
| `SPRING_PROFILES_ACTIVE` | `local` | Spring profile (`local` / `prod`) |
| `PUBLIC_API_URL` | `http://localhost:8080` | Public API URL |
| `PUBLIC_WS_URL` | `ws://localhost:8080/ws` | Public WebSocket URL |
| `MAIL_HOST` | — | SMTP host (prod only) |
| `MAIL_PORT` | `587` | SMTP port |
| `MAIL_USERNAME` | — | SMTP username |
| `MAIL_PASSWORD` | — | SMTP password |
| `SESSION_TIMEOUT` | `30m` | Session TTL |
| `COOKIE_SECURE` | `false` | `true` in production |
| `OTP_LENGTH` | `6` | OTP code length |
| `OTP_TTL_SECONDS` | `300` | OTP validity (5 min) |

Validate environment:

```bash
docker compose -f deployments/docker-compose.dev.yml config
```

---

## Deployment

### Docker Compose

```bash
# Development
make dev                    # or: ./scripts/dev-up.sh
make down                   # or: ./scripts/dev-down.sh

# Production
cp config/env/.env.example .env
# Edit .env with production values
make prod                   # or: ./scripts/prod-up.sh
make prod-down              # or: ./scripts/prod-down.sh
```

### Kubernetes

```bash
# 1. Install cert-manager (one-time)
./scripts/install-cert-manager.sh

# 2. Configure secrets
cp k8s/base/secrets.yaml k8s/base/secrets-prod.yaml
# Edit with real values: echo -n "value" | base64
mv k8s/base/secrets-prod.yaml k8s/base/secrets.yaml

# 3. Update domain in:
#    - k8s/ingress/ingress.yaml
#    - k8s/base/configmap.yaml
#    - k8s/ingress/cluster-issuer.yaml

# 4. Deploy
./scripts/k8s-deploy.sh
```

Manual deployment order:

```bash
kubectl apply -f k8s/base/namespace.yaml
kubectl apply -f k8s/base/secrets.yaml
kubectl apply -f k8s/base/configmap.yaml
kubectl apply -f k8s/postgres/
kubectl apply -f k8s/redis/
kubectl apply -f k8s/kafka/
kubectl apply -f k8s/backend/
kubectl apply -f k8s/frontend/
kubectl apply -f k8s/ingress/
```

Scaling and rollback:

```bash
kubectl scale deployment/linkwave-backend --replicas=3 -n linkwave
./scripts/k8s-rollback.sh
```

HTTPS certificates are auto-provisioned via Let's Encrypt. Requires DNS A record pointing to Traefik's external IP with port 80 open.

#### Resource Sizing

| Component | CPU Req/Lim | Memory Req/Lim |
|-----------|-------------|----------------|
| Postgres | 250m / 500m | 512Mi / 1Gi |
| Redis | 100m / 200m | 256Mi / 512Mi |
| Kafka | 250m / 500m | 512Mi / 1Gi |
| Backend | 250m / 500m | 512Mi / 1Gi |
| Frontend | 100m / 200m | 256Mi / 512Mi |

---

## CI/CD

GitHub Actions builds and publishes Docker images to GHCR on every push to `main` and on version tags.

| Event | Tags |
|-------|------|
| Push to `main` | `latest`, `sha-<hash>` |
| Tag `v1.2.3` | `1.2.3`, `1.2`, `1`, `latest` |
| PR | `pr-<number>` (build only) |

```bash
# Pull images
docker pull ghcr.io/aakashmalik/linkwave-backend:latest
docker pull ghcr.io/aakashmalik/linkwave-frontend:latest

# Create a release
git tag -a v1.0.0 -m "Release 1.0.0"
git push origin v1.0.0
```

All images are scanned with Trivy for HIGH/CRITICAL vulnerabilities. Results are uploaded to the GitHub Security tab.

---

## Observability

### Logging

| Profile | Output | Root Level | App Level |
|---------|--------|------------|-----------|
| `local` / `dev` | Colored console | INFO | DEBUG |
| `prod` | JSON (Logstash encoder) | WARN | INFO |

MDC fields auto-injected per request: `correlation_id` (UUID), `user_id` (masked phone). Requests >1s are logged at WARN. Sensitive data (phones, emails, OTPs) is masked in all environments.

```bash
# Query JSON logs
cat app.log | jq 'select(.level == "ERROR")'
cat app.log | jq 'select(.correlation_id == "<id>")'
```

Frontend uses `lib/logger.ts` — `console.*` is blocked by ESLint. Use `logger.info()`, `logger.error()`, etc.

### Metrics

Actuator endpoint: `http://localhost:8080/actuator/prometheus`

| Category | Key Metrics |
|----------|-------------|
| WebSocket | `websocket.connections.active`, `websocket.messages.total`, `websocket.errors.total` |
| Chat | `messages.sent.total`, `messages.persistence.duration`, `messages.size.bytes` |
| Kafka | `kafka.messages.produced.total`, `kafka.messages.consumed.total`, `kafka.errors.total` |
| Auth/OTP | `otp.requests.total`, `otp.verifications.total`, `otp.active.count` |
| Presence | `presence.updates.total`, `presence.heartbeat.duration` |
| Typing | `typing.events.total`, `typing.users.active` |

### Alerts

| Alert | Condition | Severity |
|-------|-----------|----------|
| `HighErrorRate` | WS errors > 1/sec for 2m | Warning |
| `HighLatency` | WS p95 > 500ms for 5m | Warning |
| `WebSocketConnectionsDrop` | Connections drop > 10 in 5m | Critical |
| `KafkaConsumerErrors` | Consumer errors > 0.1/sec for 5m | Warning |
| `RetentionJobFailure` | Any failure in 1h | Critical |

---

## Testing

```bash
# Backend
cd backend && ./gradlew test
cd backend && ./gradlew integrationTest

# Frontend
cd frontend && npm test

# All (via Make)
make test
```

---

## Pre-commit Hooks

All commits are validated by [pre-commit](https://pre-commit.com/) hooks:

| Category | Checks |
|----------|--------|
| Universal | Trailing whitespace, EOF newline, YAML/JSON syntax, large files |
| Security | Gitleaks, detect-secrets, private key detection |
| Java | Google Java Format, Checkstyle, no `System.out.println` |
| TypeScript | Prettier, ESLint, TypeScript compilation, no `console.log` |
| Commits | Conventional commits format |
| Docker | Hadolint |
| SQL | SQLFluff |
| Shell | ShellCheck, shfmt |

```bash
# Setup
./scripts/setup-pre-commit.sh

# Run manually
pre-commit run --all-files

# Skip (emergencies only)
git commit --no-verify -m "emergency fix"
```

---

## Contributing

### Setup

```bash
git clone https://github.com/AaKaSh0507/linkwave.git
cd linkwave
./scripts/setup-pre-commit.sh
make dev
```

### Workflow

1. Branch from `main`: `git checkout -b feature/LW-123-description`
2. Commit using [Conventional Commits](https://www.conventionalcommits.org/): `git commit -m "feat(chat): add message reactions"`
3. Push and open a PR
4. CI builds, pre-commit runs, code review
5. Squash and merge

### Commit Types

`feat` · `fix` · `docs` · `style` · `refactor` · `perf` · `test` · `build` · `ci` · `chore` · `revert`

### Branch Naming

```text
feature/LW-123-user-authentication
bugfix/LW-456-fix-login-error
hotfix/LW-789-security-patch
chore/update-dependencies
```

### Code Standards

**Java**: Google Java Style, SLF4J logging (never `System.out.println` or `printStackTrace()`), JUnit 5 tests.

**TypeScript**: Prettier + ESLint, no `console.log` in production code, organized imports.

---

## Troubleshooting

### Kubernetes

**Certificate stuck pending:**

```bash
kubectl describe certificate linkwave-tls-cert -n linkwave
kubectl get challenge -n linkwave
kubectl logs -n cert-manager deployment/cert-manager --tail=50
```

Common causes: DNS not resolving, port 80 blocked, wrong ingress class, rate limited.

**WebSocket 400/502:**

```bash
kubectl port-forward -n linkwave svc/linkwave-backend 8080:8080
# Test: wscat -c ws://localhost:8080/ws
```

Check: backend readiness, `/ws` path in ingress, `sessionAffinity: ClientIP`, Traefik timeout annotations.

**502 Bad Gateway:**

```bash
kubectl get endpoints linkwave-backend -n linkwave   # Should list pod IPs
kubectl logs -n linkwave deployment/linkwave-backend --tail=50
```

**Pods not starting:**

```bash
kubectl describe pod <name> -n linkwave   # ImagePullBackOff, CrashLoopBackOff
kubectl logs <name> -n linkwave --previous
kubectl get pvc -n linkwave               # PVC issues
```

**Diagnostic commands:**

```bash
kubectl get all -n linkwave
kubectl get events -n linkwave --sort-by='.lastTimestamp'
kubectl top pods -n linkwave
```

---

## License

MIT
