#!/usr/bin/env bash
# =============================================================================
# Check for Debug Code
# Detects debug statements that shouldn't be committed
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "🐛 Checking for debug code..."

ERRORS_FOUND=0

# Java debug patterns
JAVA_DEBUG_PATTERNS=(
  "System\.out\.print"
  "System\.err\.print"
  "\.printStackTrace()"
  "@Deprecated.*//.*DEBUG"
  "// DEBUG"
  "// TODO:.*DEBUG"
)

# JavaScript/TypeScript debug patterns
JS_DEBUG_PATTERNS=(
  "console\.log"
  "console\.debug"
  "console\.trace"
  "debugger;"
  "// DEBUG"
  "// TODO:.*DEBUG"
)

# Check Java files
for pattern in "${JAVA_DEBUG_PATTERNS[@]}"; do
  MATCHES=$(grep -rn "$pattern" --include="*.java" "$PROJECT_ROOT/backend/src/main/java/" 2>/dev/null | grep -v "// pre-commit:ignore" | grep -v "@SuppressWarnings" || true)
  if [[ -n "$MATCHES" ]]; then
    echo -e "${YELLOW}⚠️  Found potential debug code (pattern: $pattern):${NC}"
    echo "$MATCHES" | head -5
    if [[ $(echo "$MATCHES" | wc -l) -gt 5 ]]; then
      echo "   ... and more"
    fi
    echo ""
    # Don't fail for Java - just warn (proper logging might look similar)
  fi
done

# Check JS/TS files in production code paths
for pattern in "${JS_DEBUG_PATTERNS[@]}"; do
  MATCHES=$(grep -rn "$pattern" \
    --include="*.ts" \
    --include="*.tsx" \
    --include="*.js" \
    --include="*.jsx" \
    "$PROJECT_ROOT/frontend/app/" \
    "$PROJECT_ROOT/frontend/components/" \
    "$PROJECT_ROOT/frontend/lib/" 2>/dev/null \
    | grep -v "// eslint-disable" \
    | grep -v "// pre-commit:ignore" \
    | grep -v "\.test\." \
    | grep -v "\.spec\." \
    | grep -v "__tests__" \
    | grep -v "node_modules" \
    || true)

  if [[ -n "$MATCHES" && "$pattern" == "debugger;" ]]; then
    echo -e "${RED}❌ Found debugger statement:${NC}"
    echo "$MATCHES"
    ERRORS_FOUND=1
  elif [[ -n "$MATCHES" && "$pattern" =~ console\.(log|debug|trace) ]]; then
    echo -e "${YELLOW}⚠️  Found console statements (pattern: $pattern):${NC}"
    echo "$MATCHES" | head -5
    if [[ $(echo "$MATCHES" | wc -l) -gt 5 ]]; then
      COUNT=$(echo "$MATCHES" | wc -l)
      echo "   ... and $((COUNT - 5)) more"
    fi
    echo ""
    # Warn but don't fail for console.log (useful during development)
  fi
done

if [[ $ERRORS_FOUND -eq 1 ]]; then
  echo -e "${RED}❌ Debug code found that must be removed${NC}"
  echo "   Add '// pre-commit:ignore' comment to bypass (use sparingly)"
  exit 1
fi

echo -e "${GREEN}✓ No blocking debug code found${NC}"
exit 0
