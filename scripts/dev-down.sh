#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
echo "🛑 Stopping development environment..."
docker compose -f "$PROJECT_ROOT/deployments/docker-compose.dev.yml" down
echo "✅ Development environment stopped!"
