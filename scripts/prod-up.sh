#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
echo "🚀 Starting production environment..."
docker compose -f "$PROJECT_ROOT/deployments/docker-compose.prod.yml" up -d
echo "✅ Production environment started!"
