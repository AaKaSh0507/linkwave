# Linkwave Kubernetes Cluster Setup Guide

This comprehensive guide walks you through setting up a production-grade Kubernetes cluster for the Linkwave realtime chat application using k3s, Traefik, and Let's Encrypt TLS certificates.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [VPS Provider Selection](#vps-provider-selection)
3. [Server Initial Setup](#server-initial-setup)
4. [k3s Installation](#k3s-installation)
5. [Traefik Verification](#traefik-verification)
6. [cert-manager Installation](#cert-manager-installation)
7. [Namespace Creation](#namespace-creation)
8. [DNS Configuration](#dns-configuration)
9. [Let's Encrypt Certificate Issuers](#lets-encrypt-certificate-issuers)
10. [Testing TLS Certificates](#testing-tls-certificates)
11. [Ingress Usage Guide](#ingress-usage-guide)
12. [Cluster Management](#cluster-management)
13. [Troubleshooting](#troubleshooting)

---

## Prerequisites

Before beginning, ensure you have:

- **VPS/Cloud Server**: Ubuntu 22.04 LTS or later (recommended)
- **Minimum Resources**:
  - 2 CPU cores
  - 4GB RAM
  - 40GB disk space
  - Public IPv4 address
- **Domain Name**: Registered domain with DNS management access
- **SSH Access**: Root or sudo privileges
- **Local Tools**: `kubectl` installed on your local machine

---

## VPS Provider Selection

Recommended providers for k3s clusters:

| Provider | Starting Price | Pros | Cons |
|----------|---------------|------|------|
| **Hetzner Cloud** | €4.15/mo | Best price/performance, fast network | EU only |
| **DigitalOcean** | $12/mo | Easy to use, good documentation | Higher cost |
| **Linode (Akamai)** | $12/mo | Excellent support, global locations | Average pricing |
| **Vultr** | $6/mo | Global availability, flexible | Variable performance |
| **AWS Lightsail** | $10/mo | AWS integration, predictable pricing | Limited resources |

**Recommendation**: Hetzner Cloud CPX21 (2 vCPU, 4GB RAM, €4.15/mo) for cost-effective production deployment.

---

## Server Initial Setup

### 1. Connect to Your Server

```bash
ssh root@your-server-ip
```

### 2. Update System Packages

```bash
apt update && apt upgrade -y
```

### 3. Set Hostname

```bash
hostnamectl set-hostname linkwave-prod
```

### 4. Create Non-Root User (Optional but Recommended)

```bash
adduser linkwave
usermod -aG sudo linkwave
```

### 5. Configure Firewall

```bash
# Install UFW
apt install ufw -y

# Allow SSH, HTTP, HTTPS
ufw allow 22/tcp
ufw allow 80/tcp
ufw allow 443/tcp
ufw allow 6443/tcp  # k3s API server

# Enable firewall
ufw enable
```

### 6. Disable Swap (Required for Kubernetes)

```bash
swapoff -a
sed -i '/ swap / s/^/#/' /etc/fstab
```

---

## k3s Installation

k3s is a lightweight Kubernetes distribution perfect for single-node clusters or edge deployments.

### 1. Install k3s with Traefik

```bash
curl -sfL https://get.k3s.io | sh -s - --write-kubeconfig-mode 644
```

**What this does**:
- Installs k3s (Kubernetes v1.28+)
- Installs Traefik v2 as default ingress controller
- Configures systemd service
- Generates kubeconfig at `/etc/rancher/k3s/k3s.yaml`

### 2. Verify Installation

```bash
# Check k3s service status
systemctl status k3s

# Check node status
kubectl get nodes

# Expected output:
# NAME            STATUS   ROLES                  AGE   VERSION
# linkwave-prod   Ready    control-plane,master   30s   v1.28.x+k3s1
```

### 3. Copy kubeconfig to Local Machine

On your **local machine**:

```bash
# Copy kubeconfig from server
scp root@your-server-ip:/etc/rancher/k3s/k3s.yaml ~/.kube/linkwave-config

# Edit the server address
sed -i 's/127.0.0.1/your-server-ip/g' ~/.kube/linkwave-config

# Test connection
kubectl --kubeconfig ~/.kube/linkwave-config get nodes
```

**Optional**: Set as default kubeconfig:

```bash
export KUBECONFIG=~/.kube/linkwave-config
echo 'export KUBECONFIG=~/.kube/linkwave-config' >> ~/.zshrc
source ~/.zshrc
```

---

## Traefik Verification

k3s includes Traefik by default. Verify it's running:

```bash
kubectl get pods -n kube-system | grep traefik

# Expected output:
# traefik-xxxxx   1/1     Running   0          2m
```

### Check Traefik Service

```bash
kubectl get svc -n kube-system traefik

# Expected output:
# NAME      TYPE           CLUSTER-IP      EXTERNAL-IP      PORT(S)
# traefik   LoadBalancer   10.43.x.x       your-server-ip   80:xxxxx/TCP,443:xxxxx/TCP
```

The `EXTERNAL-IP` should match your server's public IP address.

---

## cert-manager Installation

cert-manager automates TLS certificate provisioning with Let's Encrypt.

### 1. Install cert-manager

```bash
# Install cert-manager CRDs
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.14.1/cert-manager.crds.yaml

# Create cert-manager namespace
kubectl create namespace cert-manager

# Install cert-manager
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.14.1/cert-manager.yaml
```

### 2. Verify Installation

```bash
kubectl get pods -n cert-manager

# Expected output (all pods should be Running):
# NAME                                       READY   STATUS    RESTARTS   AGE
# cert-manager-xxxxx                         1/1     Running   0          30s
# cert-manager-cainjector-xxxxx              1/1     Running   0          30s
# cert-manager-webhook-xxxxx                 1/1     Running   0          30s
```

### 3. Wait for Webhook Readiness

```bash
kubectl wait --for=condition=Available --timeout=300s \
  deployment/cert-manager-webhook -n cert-manager
```

---

## Namespace Creation

### 1. Clone Repository

```bash
git clone https://github.com/AaKaSh0507/linkwave.git
cd linkwave
```

### 2. Create Linkwave Namespace

```bash
kubectl apply -f k8s/namespace.yaml
```

### 3. Verify Namespace

```bash
kubectl get namespaces | grep linkwave

# Expected output:
# linkwave   Active   5s
```

---

## DNS Configuration

Before deploying ingress resources, configure DNS records.

### 1. Identify Required Domains

For the Linkwave application, you'll need:

- **Frontend**: `linkwave.example.com`
- **Backend API**: `api.linkwave.example.com`
- **WebSocket**: `ws.linkwave.example.com`

### 2. Create DNS A Records

In your domain registrar's DNS management panel, create the following records:

| Type | Name | Value | TTL |
|------|------|-------|-----|
| A | `@` | `your-server-ip` | 3600 |
| A | `api` | `your-server-ip` | 3600 |
| A | `ws` | `your-server-ip` | 3600 |

**Example** (using example.com):
- `linkwave.example.com` → `123.456.789.0`
- `api.linkwave.example.com` → `123.456.789.0`
- `ws.linkwave.example.com` → `123.456.789.0`

### 3. Verify DNS Propagation

```bash
# Test DNS resolution (replace with your domain)
dig linkwave.example.com +short
dig api.linkwave.example.com +short
dig ws.linkwave.example.com +short

# All should return your-server-ip
```

**Note**: DNS propagation can take 5 minutes to 48 hours depending on TTL and registrar.

---

## Let's Encrypt Certificate Issuers

### 1. Configure Email Address

Edit both ClusterIssuer manifests:

```bash
# Staging issuer
vim k8s/letsencrypt-staging-issuer.yaml

# Production issuer
vim k8s/letsencrypt-prod-issuer.yaml
```

Replace `your-email@example.com` with your actual email address:

```yaml
spec:
  acme:
    email: your-email@example.com  # <-- Change this
```

### 2. Apply Staging Issuer First

**Always test with staging issuer first** to avoid hitting Let's Encrypt rate limits (5 certificates per week per domain).

```bash
kubectl apply -f k8s/letsencrypt-staging-issuer.yaml
```

### 3. Verify Staging Issuer

```bash
kubectl get clusterissuer

# Expected output:
# NAME                  READY   AGE
# letsencrypt-staging   True    10s
```

Check the issuer status:

```bash
kubectl describe clusterissuer letsencrypt-staging
```

Look for `Status: Ready` in the output.

### 4. Apply Production Issuer

Once staging is verified, deploy the production issuer:

```bash
kubectl apply -f k8s/letsencrypt-prod-issuer.yaml
```

### 5. Verify Production Issuer

```bash
kubectl get clusterissuer

# Expected output:
# NAME                  READY   AGE
# letsencrypt-staging   True    2m
# letsencrypt-prod      True    5s
```

---

## Testing TLS Certificates

### 1. Deploy Test Ingress

Create a test deployment to verify TLS certificate provisioning:

```bash
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: Pod
metadata:
  name: nginx-test
  namespace: linkwave
  labels:
    app: nginx-test
spec:
  containers:
    - name: nginx
      image: nginx:alpine
      ports:
        - containerPort: 80
---
apiVersion: v1
kind: Service
metadata:
  name: nginx-test
  namespace: linkwave
spec:
  selector:
    app: nginx-test
  ports:
    - port: 80
      targetPort: 80
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: nginx-test-ingress
  namespace: linkwave
  annotations:
    kubernetes.io/ingress.class: traefik
    cert-manager.io/cluster-issuer: letsencrypt-staging
spec:
  tls:
    - hosts:
        - test.linkwave.example.com
      secretName: test-tls
  rules:
    - host: test.linkwave.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: nginx-test
                port:
                  number: 80
EOF
```

**Replace** `test.linkwave.example.com` with your actual domain.

### 2. Monitor Certificate Provisioning

```bash
# Watch certificate request
kubectl get certificate -n linkwave --watch

# Expected output:
# NAME       READY   SECRET     AGE
# test-tls   True    test-tls   45s
```

### 3. Check Certificate Details

```bash
kubectl describe certificate test-tls -n linkwave
```

Look for:
- `Status: True`
- `Message: Certificate is up to date and has not expired`

### 4. Test in Browser

Visit `https://test.linkwave.example.com` in your browser.

**Expected**:
- ⚠️ Browser shows "Your connection is not private" (expected for staging certificates)
- Click "Advanced" → "Proceed to test.linkwave.example.com"
- You should see the nginx welcome page

### 5. Switch to Production Certificate

Once staging works, update the ingress:

```bash
kubectl edit ingress nginx-test-ingress -n linkwave
```

Change:
```yaml
cert-manager.io/cluster-issuer: letsencrypt-staging
```

To:
```yaml
cert-manager.io/cluster-issuer: letsencrypt-prod
```

Wait 30-60 seconds and refresh the browser. The certificate should now be valid (no warnings).

### 6. Clean Up Test Resources

```bash
kubectl delete ingress nginx-test-ingress -n linkwave
kubectl delete service nginx-test -n linkwave
kubectl delete pod nginx-test -n linkwave
kubectl delete certificate test-tls -n linkwave
kubectl delete secret test-tls -n linkwave
```

---

## Ingress Usage Guide

The `k8s/ingress-examples.yaml` file contains three ingress templates for the Linkwave application.

### Frontend Ingress (Static Assets)

**Purpose**: Serve Vue.js frontend static files

**Configuration**:
```yaml
# Edit ingress-examples.yaml
spec:
  tls:
    - hosts:
        - linkwave.example.com  # Replace with your domain
```

**Deploy**:
```bash
# After deploying frontend Deployment and Service
kubectl apply -f k8s/ingress-examples.yaml
```

**Access**: `https://linkwave.example.com`

### Backend API Ingress (REST)

**Purpose**: Route REST API requests to Spring Boot backend

**Configuration**:
```yaml
spec:
  tls:
    - hosts:
        - api.linkwave.example.com  # Replace with your domain
```

**Deploy**:
```bash
kubectl apply -f k8s/ingress-examples.yaml
```

**Access**: `https://api.linkwave.example.com/api/...`

### WebSocket Ingress (Realtime Chat)

**Purpose**: Handle WebSocket connections for realtime messaging

**Configuration**:
```yaml
spec:
  tls:
    - hosts:
        - ws.linkwave.example.com  # Replace with your domain
```

**Deploy**:
```bash
kubectl apply -f k8s/ingress-examples.yaml
```

**Access**: `wss://ws.linkwave.example.com/ws`

### Switching Between Staging and Production

To test with staging certificates:
```yaml
annotations:
  cert-manager.io/cluster-issuer: letsencrypt-staging
```

For production:
```yaml
annotations:
  cert-manager.io/cluster-issuer: letsencrypt-prod
```

---

## Cluster Management

### Viewing Cluster Resources

```bash
# All resources in linkwave namespace
kubectl get all -n linkwave

# Ingress resources
kubectl get ingress -n linkwave

# Certificates
kubectl get certificate -n linkwave

# Secrets (includes TLS certificates)
kubectl get secrets -n linkwave
```

### Monitoring Logs

```bash
# Traefik logs
kubectl logs -n kube-system -l app.kubernetes.io/name=traefik --tail=100 -f

# cert-manager logs
kubectl logs -n cert-manager -l app=cert-manager --tail=100 -f

# Application logs (after deployment)
kubectl logs -n linkwave -l app=linkwave-backend --tail=100 -f
```

### Updating Configurations

```bash
# Apply changes to existing resources
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/letsencrypt-prod-issuer.yaml
kubectl apply -f k8s/ingress-examples.yaml

# Restart a deployment (triggers rolling update)
kubectl rollout restart deployment/linkwave-backend -n linkwave
```

### Scaling Applications

```bash
# Scale backend replicas
kubectl scale deployment/linkwave-backend --replicas=3 -n linkwave

# Scale frontend replicas
kubectl scale deployment/linkwave-frontend --replicas=2 -n linkwave
```

### Cluster Upgrades

```bash
# Upgrade k3s to latest version
curl -sfL https://get.k3s.io | sh -

# Verify upgrade
kubectl version --short
```

---

## Troubleshooting

### Issue: Certificate Stuck in "Pending"

**Symptoms**:
```bash
kubectl get certificate -n linkwave
# NAME       READY   SECRET     AGE
# test-tls   False   test-tls   5m
```

**Diagnosis**:
```bash
kubectl describe certificate test-tls -n linkwave
kubectl describe certificaterequest -n linkwave
kubectl describe challenge -n linkwave
```

**Common Causes**:
1. **DNS not propagated**: Wait 10-15 minutes and retry
2. **Firewall blocking port 80**: Ensure port 80 is open for HTTP-01 challenge
3. **Incorrect email**: Check ClusterIssuer email configuration

**Solution**:
```bash
# Delete and recreate certificate
kubectl delete certificate test-tls -n linkwave
kubectl delete ingress nginx-test-ingress -n linkwave
kubectl apply -f k8s/ingress-examples.yaml
```

### Issue: Traefik Not Routing Traffic

**Symptoms**: Ingress created but domain returns 404 or connection refused.

**Diagnosis**:
```bash
# Check Traefik service
kubectl get svc -n kube-system traefik

# Check ingress
kubectl describe ingress -n linkwave
```

**Solution**:
```bash
# Restart Traefik
kubectl rollout restart deployment/traefik -n kube-system

# Verify ingress class annotation
kubectl edit ingress your-ingress -n linkwave
# Ensure: kubernetes.io/ingress.class: traefik
```

### Issue: k3s Service Not Starting

**Symptoms**:
```bash
systemctl status k3s
# Active: failed (Result: exit-code)
```

**Diagnosis**:
```bash
journalctl -u k3s -n 100 --no-pager
```

**Common Causes**:
1. **Swap enabled**: Disable swap with `swapoff -a`
2. **Port conflict**: Check if port 6443 is in use
3. **Insufficient resources**: Ensure 4GB RAM minimum

**Solution**:
```bash
# Reset k3s installation
/usr/local/bin/k3s-uninstall.sh

# Reinstall
curl -sfL https://get.k3s.io | sh -s - --write-kubeconfig-mode 644
```

### Issue: Let's Encrypt Rate Limit

**Symptoms**: Certificate fails with error "too many certificates already issued".

**Rate Limits**:
- **Staging**: 30,000 certificates per week (essentially unlimited)
- **Production**: 5 certificates per week per registered domain

**Solution**:
```bash
# Always test with staging first
kubectl apply -f k8s/letsencrypt-staging-issuer.yaml

# Edit ingress to use staging
kubectl edit ingress your-ingress -n linkwave
# Change: cert-manager.io/cluster-issuer: letsencrypt-staging

# After verifying staging works, switch to production
# Change: cert-manager.io/cluster-issuer: letsencrypt-prod
```

### Issue: WebSocket Connection Fails

**Symptoms**: REST API works but WebSocket connections fail with 400/426 errors.

**Diagnosis**:
```bash
# Check ingress annotations
kubectl get ingress linkwave-websocket-ingress -n linkwave -o yaml
```

**Solution**:
Ensure WebSocket ingress has these annotations:
```yaml
annotations:
  traefik.ingress.kubernetes.io/websocket: "true"
  traefik.ingress.kubernetes.io/session-affinity: "true"
```

### Issue: Ingress Shows No Endpoints

**Symptoms**:
```bash
kubectl describe ingress your-ingress -n linkwave
# Endpoints: <none>
```

**Diagnosis**:
```bash
# Check service
kubectl get service -n linkwave

# Check pod labels
kubectl get pods -n linkwave --show-labels
```

**Solution**:
Ensure service selector matches pod labels:
```yaml
# Service
spec:
  selector:
    app: linkwave-backend

# Deployment
spec:
  template:
    metadata:
      labels:
        app: linkwave-backend
```

### Getting Help

**Check cert-manager logs**:
```bash
kubectl logs -n cert-manager -l app=cert-manager --tail=200
```

**Check Traefik logs**:
```bash
kubectl logs -n kube-system -l app.kubernetes.io/name=traefik --tail=200
```

**Describe all resources**:
```bash
kubectl describe all -n linkwave
```

**Export kubeconfig for support**:
```bash
kubectl cluster-info dump > cluster-debug.txt
```

---

## Next Steps

After completing cluster setup:

1. **Deploy Application Workloads**:
   - Create Kubernetes Deployments for backend and frontend
   - Create Services to expose pods internally
   - Apply ingress resources to expose services externally

2. **Database Setup**:
   - Deploy PostgreSQL using StatefulSet or external managed database
   - Configure persistent volumes for data durability
   - Set up regular backups

3. **Monitoring & Observability**:
   - Install Prometheus for metrics collection
   - Deploy Grafana for visualization
   - Configure alerts for critical events

4. **CI/CD Integration**:
   - Set up GitHub Actions for automated deployments
   - Configure image registry (Docker Hub, GitHub Container Registry)
   - Implement rolling updates with zero downtime

5. **Security Hardening**:
   - Enable RBAC policies
   - Configure Network Policies
   - Implement Pod Security Standards
   - Set up regular security scanning

---

## References

- **k3s Documentation**: https://docs.k3s.io/
- **Traefik Documentation**: https://doc.traefik.io/traefik/
- **cert-manager Documentation**: https://cert-manager.io/docs/
- **Let's Encrypt Rate Limits**: https://letsencrypt.org/docs/rate-limits/
- **Kubernetes Ingress**: https://kubernetes.io/docs/concepts/services-networking/ingress/

---

**Document Version**: 1.0  
**Last Updated**: 2024  
**Maintained By**: Linkwave Team
