# Kubernetes Deployment Guide

## Prerequisites

- **k3s** cluster running with `kubectl` configured
- Docker images pushed to GHCR: `ghcr.io/aakashmalik/linkwave-backend:latest`, `ghcr.io/aakashmalik/linkwave-frontend:latest`

## Quick Deploy

```bash
# 1. Install cert-manager (one-time)
chmod +x k8s/install-cert-manager.sh
./k8s/install-cert-manager.sh

# 2. Create secrets (edit with real base64 values first)
cp k8s/base/secrets.yaml k8s/base/secrets-prod.yaml
# Edit with real values: echo -n "value" | base64
mv k8s/base/secrets-prod.yaml k8s/base/secrets.yaml

# 3. Update domain (replace linkwave.example.com)
# - k8s/ingress/ingress.yaml
# - k8s/base/configmap.yaml (NEXT_PUBLIC_API_URL, NEXT_PUBLIC_WS_URL)
# - k8s/ingress/cluster-issuer.yaml (email)

# 4. Deploy everything
chmod +x k8s/deploy.sh
./k8s/deploy.sh
```

## Manual Deployment Order

```bash
# 1. Base
kubectl apply -f k8s/base/namespace.yaml
kubectl apply -f k8s/base/secrets.yaml
kubectl apply -f k8s/base/configmap.yaml

# 2. Infrastructure
kubectl apply -f k8s/postgres/
kubectl apply -f k8s/redis/
kubectl apply -f k8s/kafka/

# 3. Application
kubectl apply -f k8s/backend/
kubectl apply -f k8s/frontend/

# 4. Ingress & TLS
kubectl apply -f k8s/ingress/
```

## HTTPS & TLS Setup

### cert-manager Installation

```bash
./k8s/install-cert-manager.sh

# Verify
kubectl get pods -n cert-manager
kubectl get clusterissuer
```

### Certificate Issuance

Certificates are automatically provisioned via Let's Encrypt when the ingress is applied. Requirements:

1. DNS A record pointing to Traefik's external IP
2. Port 80 open for HTTP-01 challenge validation

```bash
# Get Traefik external IP
kubectl get svc traefik -n kube-system -o jsonpath='{.status.loadBalancer.ingress[0].ip}'

# Check certificate status
kubectl get certificate -n linkwave
kubectl describe certificate linkwave-tls-cert -n linkwave

# Verify issued certificate
kubectl get secret linkwave-tls-cert -n linkwave -o jsonpath='{.data.tls\.crt}' | \
  base64 -d | openssl x509 -text -noout | head -20
```

### Staging vs Production

Use staging first to avoid Let's Encrypt rate limits:

```bash
# Switch to staging (in k8s/ingress/ingress.yaml)
# Change annotation: cert-manager.io/cluster-issuer: letsencrypt-staging

# Once validated, switch back to production:
# cert-manager.io/cluster-issuer: letsencrypt-prod

# Delete old cert to re-issue
kubectl delete certificate linkwave-tls-cert -n linkwave
kubectl delete secret linkwave-tls-cert -n linkwave
```

## WebSocket (WSS) Configuration

WebSocket connections are handled natively by Traefik. The setup includes:

- **Session affinity** (`ClientIP`) on the backend service for sticky WebSocket connections
- **X-Forwarded-Proto** header injection so the backend knows traffic is HTTPS-terminated
- No forced `Connection: Upgrade` headers — Traefik upgrades automatically when the client sends a WebSocket request

### Testing WebSocket

```bash
# Local test via port-forward
kubectl port-forward -n linkwave svc/linkwave-backend 8080:8080
wscat -c ws://localhost:8080/ws

# Test over HTTPS (after domain+cert are ready)
wscat -c wss://your-domain.com/ws
```

## Security Features

| Feature | Description |
| ------- | ----------- |
| TLS 1.2+ | Minimum TLS 1.2 with modern cipher suites |
| HSTS | `max-age=31536000; includeSubDomains; preload` |
| HTTP→HTTPS | Permanent 301 redirect via Traefik middleware |
| Security headers | `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, XSS filter |
| Server stripping | `Server` and `X-Powered-By` headers removed |
| SNI strict | Reject connections without matching hostname |

## Service Discovery (Internal DNS)

| Service | DNS Name | Port |
| ------- | -------- | ---- |
| Postgres | `postgres.linkwave.svc.cluster.local` | 5432 |
| Redis | `redis.linkwave.svc.cluster.local` | 6379 |
| Kafka | `kafka.linkwave.svc.cluster.local` | 9092 |
| Zookeeper | `zookeeper.linkwave.svc.cluster.local` | 2181 |
| Backend | `linkwave-backend.linkwave.svc.cluster.local` | 8080 |
| Frontend | `linkwave-frontend.linkwave.svc.cluster.local` | 3000 |

## Scaling

```bash
kubectl scale deployment/linkwave-backend --replicas=3 -n linkwave
kubectl scale deployment/linkwave-frontend --replicas=3 -n linkwave
```

## Rollback

```bash
# Quick rollback
chmod +x k8s/rollback.sh
./k8s/rollback.sh

# Manual / specific revision
kubectl rollout undo deployment/linkwave-backend -n linkwave --to-revision=2
```

## Resource Sizing

| Component | CPU Req/Lim | Memory Req/Lim |
| --------- | ----------- | -------------- |
| Postgres | 250m / 500m | 512Mi / 1Gi |
| Redis | 100m / 200m | 256Mi / 512Mi |
| Kafka | 250m / 500m | 512Mi / 1Gi |
| Zookeeper | 100m / 250m | 256Mi / 512Mi |
| Backend | 250m / 500m | 512Mi / 1Gi |
| Frontend | 100m / 200m | 256Mi / 512Mi |

## Configuration

### Updating Domain

Replace `linkwave.example.com` in:

- `k8s/ingress/ingress.yaml` (host + TLS, both ingress resources)
- `k8s/base/configmap.yaml` (`NEXT_PUBLIC_API_URL`, `NEXT_PUBLIC_WS_URL`)
- `k8s/ingress/cluster-issuer.yaml` (email)

### Secrets

```bash
echo -n "your-real-password" | base64
```

For production, use [Sealed Secrets](https://github.com/bitnami-labs/sealed-secrets) or [External Secrets Operator](https://external-secrets.io/).

## Troubleshooting

See [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) for detailed guides on:

- Certificate pending issues
- WebSocket connection failures
- HTTP 502/503 errors
- Pod startup problems
- HTTPS redirect loops
