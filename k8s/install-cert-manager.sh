#!/usr/bin/env bash
set -euo pipefail

CERT_MANAGER_VERSION="v1.14.0"

echo "========================================="
echo "  cert-manager Installation"
echo "========================================="

# Check if already installed
if kubectl get namespace cert-manager &>/dev/null; then
  echo "ℹ️  cert-manager namespace exists. Checking pods..."
  if kubectl get pods -n cert-manager -l app.kubernetes.io/instance=cert-manager --no-headers 2>/dev/null | grep -q Running; then
    echo "✅ cert-manager is already installed and running."
    kubectl get pods -n cert-manager
    echo ""
    echo "To reinstall, delete first: kubectl delete -f https://github.com/cert-manager/cert-manager/releases/download/${CERT_MANAGER_VERSION}/cert-manager.yaml"
    exit 0
  fi
fi

# Install cert-manager
echo ""
echo "📦 Installing cert-manager ${CERT_MANAGER_VERSION}..."
kubectl apply -f "https://github.com/cert-manager/cert-manager/releases/download/${CERT_MANAGER_VERSION}/cert-manager.yaml"

# Wait for pods
echo ""
echo "⏳ Waiting for cert-manager pods to be ready (up to 5 minutes)..."
kubectl wait --for=condition=Ready pod \
  -l app.kubernetes.io/instance=cert-manager \
  -n cert-manager \
  --timeout=300s

# Verify webhook
echo ""
echo "🔍 Verifying webhook configuration..."
kubectl get validatingwebhookconfiguration | grep cert-manager || true

# Check logs for errors
echo ""
echo "📋 cert-manager controller logs (last 10 lines):"
kubectl logs -n cert-manager deployment/cert-manager --tail=10

echo ""
echo "========================================="
echo "  ✅ cert-manager installed successfully!"
echo "========================================="
echo ""
echo "Next steps:"
echo "  1. Apply ClusterIssuers: kubectl apply -f k8s/ingress/cluster-issuer.yaml"
echo "  2. Verify: kubectl get clusterissuer"
echo "  3. Check status: kubectl describe clusterissuer letsencrypt-prod"
