# Kubernetes Troubleshooting Guide

## Certificate Issues

### Certificate Stuck in "Pending"

```bash
# Check certificate status
kubectl describe certificate linkwave-tls-cert -n linkwave

# Check certificate request
kubectl get certificaterequest -n linkwave
kubectl describe certificaterequest -n linkwave

# Check ACME order and challenge
kubectl get order -n linkwave
kubectl describe order -n linkwave
kubectl get challenge -n linkwave
kubectl describe challenge -n linkwave

# Check cert-manager logs
kubectl logs -n cert-manager deployment/cert-manager --tail=50
```

**Common causes:**

| Cause | Fix |
| ----- | --- |
| DNS not pointing to cluster | Create A record pointing to Traefik external IP |
| Port 80 blocked | Ensure HTTP-01 challenge can reach port 80 |
| Wrong ingress class | Verify `class: traefik` in ClusterIssuer solver |
| Rate limited (prod) | Switch to `letsencrypt-staging` issuer, wait 1 hour |

### Certificate Renewal Failures

```bash
# Check certificate expiry
kubectl get certificate -n linkwave -o jsonpath='{.items[*].status.notAfter}'

# Force renewal
kubectl delete certificate linkwave-tls-cert -n linkwave
# cert-manager will recreate it from the Ingress annotation
```

---

## WebSocket Connection Failures

### Handshake Fails with 400/502

```bash
# Verify backend is running
kubectl get pods -n linkwave -l app=linkwave-backend

# Test WebSocket locally via port-forward
kubectl port-forward -n linkwave svc/linkwave-backend 8080:8080
# In another terminal: wscat -c ws://localhost:8080/ws

# Check Traefik logs
kubectl logs -n kube-system deploy/traefik --tail=50

# Verify middleware
kubectl get middleware -n linkwave
kubectl describe middleware websocket-headers -n linkwave
```

**Common causes:**

| Cause | Fix |
| ----- | --- |
| Backend not ready | Check `kubectl get pods -n linkwave` |
| Wrong path | Verify `/ws` path in ingress.yaml |
| Session affinity missing | Ensure `sessionAffinity: ClientIP` on backend service |
| Timeout too short | Check Traefik timeout annotations |

### WebSocket Drops After 30s

Traefik default idle timeout is 30s. The ingress should have extended timeouts — verify annotations are applied:

```bash
kubectl get ingress linkwave-ingress -n linkwave -o yaml | grep -A2 timeout
```

---

## HTTP 502 Bad Gateway

```bash
# Check if backend pods are running
kubectl get pods -n linkwave -l app=linkwave-backend -o wide

# Check backend logs
kubectl logs -n linkwave deployment/linkwave-backend --tail=50

# Check endpoints (should list pod IPs)
kubectl get endpoints linkwave-backend -n linkwave

# Verify service selector
kubectl describe svc linkwave-backend -n linkwave
```

**Common causes:**

| Cause | Fix |
| ----- | --- |
| No healthy pods | Check readiness probes, resource limits |
| Wrong port | Verify service targets port 8080 |
| No endpoints | Selector labels don't match pod labels |
| Init containers stuck | Check `kubectl describe pod <name> -n linkwave` |

---

## HTTP 503 Service Unavailable

```bash
# Check if service exists
kubectl get svc -n linkwave

# Check ingress backend
kubectl describe ingress linkwave-ingress -n linkwave
```

---

## Pods Not Starting

### ImagePullBackOff

```bash
kubectl describe pod <pod-name> -n linkwave | grep -A5 Events
```

Fix: Verify image name is correct, GHCR is accessible, and image exists.

### CrashLoopBackOff

```bash
# Check current logs
kubectl logs <pod-name> -n linkwave

# Check previous crash logs
kubectl logs <pod-name> -n linkwave --previous

# Check resource limits
kubectl describe pod <pod-name> -n linkwave | grep -A5 Limits
```

### Pending (PVC Issues)

```bash
kubectl get pvc -n linkwave
kubectl describe pvc <pvc-name> -n linkwave
kubectl get storageclass
```

Fix: Verify `local-path` StorageClass exists (`kubectl get sc`).

---

## HTTPS Redirect Loop

If you get infinite redirects:

```bash
# Verify two separate ingress resources exist
kubectl get ingress -n linkwave
# Should show: linkwave-ingress (websecure) + linkwave-ingress-redirect (web)

# Check entrypoint annotations
kubectl get ingress linkwave-ingress -n linkwave -o yaml | grep entrypoints
kubectl get ingress linkwave-ingress-redirect -n linkwave -o yaml | grep entrypoints
```

Fix: Ensure the main ingress uses `websecure` entrypoint and redirect ingress uses `web`.

---

## Diagnostic Commands

```bash
# Full cluster overview
kubectl get all -n linkwave

# Events (sorted by time)
kubectl get events -n linkwave --sort-by='.lastTimestamp'

# Resource usage
kubectl top pods -n linkwave
kubectl top nodes

# Shell into a pod
kubectl exec -it <pod-name> -n linkwave -- /bin/sh

# Port forwarding
kubectl port-forward -n linkwave svc/linkwave-backend 8080:8080
kubectl port-forward -n linkwave svc/linkwave-frontend 3000:3000
kubectl port-forward -n kube-system svc/traefik 8443:443

# DNS resolution test from inside cluster
kubectl run dns-test --rm -it --image=busybox -- nslookup postgres.linkwave.svc.cluster.local
```
