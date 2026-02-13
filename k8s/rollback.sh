#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="linkwave"

echo "========================================="
echo "  LinkWave Kubernetes Rollback"
echo "========================================="

rollback() {
  local deployment=$1
  echo ""
  echo "🔄 Rolling back $deployment..."
  kubectl rollout undo deployment/"$deployment" -n "$NAMESPACE"
  echo "⏳ Waiting for rollout to complete..."
  kubectl rollout status deployment/"$deployment" -n "$NAMESPACE" --timeout=120s
  echo "✅ $deployment rolled back successfully."
}

# Rollback application deployments (not infrastructure)
rollback "linkwave-backend"
rollback "linkwave-frontend"

echo ""
echo "========================================="
echo "  ✅ Rollback Complete!"
echo "========================================="
echo ""
kubectl get pods -n "$NAMESPACE" -o wide
