#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
echo "🚀 Starting development environment..."
docker compose -f "$PROJECT_ROOT/deployments/docker-compose.dev.yml" up -d
echo "✅ Development environment started!"
