#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="linkwave"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "========================================="
echo "  LinkWave Kubernetes Deployment"
echo "========================================="

# --- Helpers ---
wait_for_pods() {
  local label=$1
  local timeout=${2:-120}
  echo "⏳ Waiting for pods with label '$label' to be ready (${timeout}s timeout)..."
  kubectl wait --for=condition=Ready pod -l "$label" -n "$NAMESPACE" --timeout="${timeout}s"
  echo "✅ Pods with label '$label' are ready."
}

apply() {
  local file=$1
  echo "📦 Applying $file..."
  kubectl apply -f "$file"
}

# --- 1. Namespace ---
echo ""
echo "--- Step 1: Namespace ---"
apply "$SCRIPT_DIR/base/namespace.yaml"

# --- 2. Secrets ---
echo ""
echo "--- Step 2: Secrets ---"
if [ -f "$SCRIPT_DIR/base/secrets.yaml" ]; then
  apply "$SCRIPT_DIR/base/secrets.yaml"
else
  echo "⚠️  secrets.yaml not found. Create it from the template before deploying."
  echo "   See k8s/README.md for instructions."
  exit 1
fi

# --- 3. ConfigMap ---
echo ""
echo "--- Step 3: ConfigMap ---"
apply "$SCRIPT_DIR/base/configmap.yaml"

# --- 4. Postgres ---
echo ""
echo "--- Step 4: Postgres ---"
apply "$SCRIPT_DIR/postgres/postgres-pvc.yaml"
apply "$SCRIPT_DIR/postgres/postgres-deployment.yaml"
apply "$SCRIPT_DIR/postgres/postgres-service.yaml"
wait_for_pods "app=postgres" 120

# --- 5. Redis ---
echo ""
echo "--- Step 5: Redis ---"
apply "$SCRIPT_DIR/redis/redis-deployment.yaml"
apply "$SCRIPT_DIR/redis/redis-service.yaml"
wait_for_pods "app=redis" 60

# --- 6. Kafka (Zookeeper first) ---
echo ""
echo "--- Step 6: Kafka ---"
apply "$SCRIPT_DIR/kafka/zookeeper-deployment.yaml"
apply "$SCRIPT_DIR/kafka/zookeeper-service.yaml"
wait_for_pods "app=zookeeper" 60

apply "$SCRIPT_DIR/kafka/kafka-pvc.yaml"
apply "$SCRIPT_DIR/kafka/kafka-deployment.yaml"
apply "$SCRIPT_DIR/kafka/kafka-service.yaml"
wait_for_pods "app=kafka" 120

# --- 7. Backend ---
echo ""
echo "--- Step 7: Backend ---"
apply "$SCRIPT_DIR/backend/backend-deployment.yaml"
apply "$SCRIPT_DIR/backend/backend-service.yaml"
wait_for_pods "app=linkwave-backend" 180

# --- 8. Frontend ---
echo ""
echo "--- Step 8: Frontend ---"
apply "$SCRIPT_DIR/frontend/frontend-deployment.yaml"
apply "$SCRIPT_DIR/frontend/frontend-service.yaml"
wait_for_pods "app=linkwave-frontend" 120

# --- 9. Ingress ---
echo ""
echo "--- Step 9: Ingress, Middleware & TLS ---"
apply "$SCRIPT_DIR/ingress/websocket-middleware.yaml"
apply "$SCRIPT_DIR/ingress/hsts-middleware.yaml"
apply "$SCRIPT_DIR/ingress/https-redirect-middleware.yaml"
apply "$SCRIPT_DIR/ingress/tls-options.yaml"
apply "$SCRIPT_DIR/ingress/ingress.yaml"

# cert-manager ClusterIssuer (only if cert-manager is installed)
if kubectl get crd clusterissuers.cert-manager.io &>/dev/null; then
  apply "$SCRIPT_DIR/ingress/cluster-issuer.yaml"
  echo ""
  echo "🔍 Waiting for certificate issuance..."
  kubectl wait --for=condition=Ready certificate linkwave-tls-cert \
    -n "$NAMESPACE" --timeout=300s 2>/dev/null \
    || echo "⚠️  Certificate not ready yet. It will be issued once DNS is configured."
else
  echo "⚠️  cert-manager not found. Run './k8s/install-cert-manager.sh' first for TLS."
fi

# --- Summary ---
echo ""
echo "========================================="
echo "  ✅ Deployment Complete!"
echo "========================================="
echo ""
kubectl get pods -n "$NAMESPACE" -o wide
echo ""
echo "Verify with:"
echo "  kubectl get all -n $NAMESPACE"
echo "  kubectl logs -f deployment/linkwave-backend -n $NAMESPACE"
echo "  kubectl logs -f deployment/linkwave-frontend -n $NAMESPACE"
