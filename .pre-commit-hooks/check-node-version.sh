#!/usr/bin/env bash
# =============================================================================
# Check Node.js Version Compatibility
# Ensures Node.js version meets project requirements
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "📦 Checking Node.js version compatibility..."

# Minimum required Node.js version
MIN_NODE_VERSION="20.0.0"

# Function to compare versions
version_gte() {
  printf '%s\n%s' "$2" "$1" | sort -V -C
}

# Get current Node.js version
if command -v node &>/dev/null; then
  CURRENT_VERSION=$(node -v | sed 's/v//')

  if version_gte "$CURRENT_VERSION" "$MIN_NODE_VERSION"; then
    echo -e "${GREEN}✓ Node.js version: $CURRENT_VERSION (>= $MIN_NODE_VERSION)${NC}"
  else
    echo -e "${RED}❌ Node.js version too old:${NC}"
    echo -e "   Current:  ${RED}$CURRENT_VERSION${NC}"
    echo -e "   Required: ${GREEN}>= $MIN_NODE_VERSION${NC}"
    echo ""
    echo "   Please upgrade Node.js: https://nodejs.org/"
    exit 1
  fi
else
  echo -e "${YELLOW}⚠️  Node.js not found in PATH${NC}"
  echo "   Install Node.js: https://nodejs.org/"
  exit 0
fi

# Check .nvmrc if exists
if [[ -f "$PROJECT_ROOT/frontend/.nvmrc" ]]; then
  NVMRC_VERSION=$(cat "$PROJECT_ROOT/frontend/.nvmrc" | tr -d 'v')
  echo -e "${GREEN}✓ .nvmrc specifies Node.js: $NVMRC_VERSION${NC}"
fi

# Check package.json engines
if [[ -f "$PROJECT_ROOT/frontend/package.json" ]]; then
  ENGINES_NODE=$(grep -oP '"node"\s*:\s*"\K[^"]+' "$PROJECT_ROOT/frontend/package.json" 2>/dev/null || echo "")
  if [[ -n "$ENGINES_NODE" ]]; then
    echo -e "${GREEN}✓ package.json engines.node: $ENGINES_NODE${NC}"
  fi
fi

exit 0
