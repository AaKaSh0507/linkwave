#!/usr/bin/env bash
# =============================================================================
# Check License Headers
# Ensures source files have proper license headers
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "📄 Checking license headers..."

# Expected license header (modify as needed)
LICENSE_PATTERN="Copyright.*LinkWave"

# Alternatively, check for any copyright notice
GENERIC_PATTERN="Copyright|License|MIT|Apache|GPL|BSD"

MISSING_COUNT=0
CHECKED_COUNT=0

# Check Java files
while IFS= read -r -d '' file; do
    CHECKED_COUNT=$((CHECKED_COUNT + 1))
    if ! head -20 "$file" | grep -qiE "$GENERIC_PATTERN"; then
        if [[ $MISSING_COUNT -eq 0 ]]; then
            echo -e "${YELLOW}⚠️  Files missing license headers:${NC}"
        fi
        echo "   - ${file#$PROJECT_ROOT/}"
        MISSING_COUNT=$((MISSING_COUNT + 1))
        if [[ $MISSING_COUNT -ge 10 ]]; then
            echo "   ... and more"
            break
        fi
    fi
done < <(find "$PROJECT_ROOT/backend/src/main/java" -name "*.java" -print0 2>/dev/null || true)

# Check TypeScript files
while IFS= read -r -d '' file; do
    CHECKED_COUNT=$((CHECKED_COUNT + 1))
    if ! head -20 "$file" | grep -qiE "$GENERIC_PATTERN"; then
        if [[ $MISSING_COUNT -eq 0 ]]; then
            echo -e "${YELLOW}⚠️  Files missing license headers:${NC}"
        fi
        echo "   - ${file#$PROJECT_ROOT/}"
        MISSING_COUNT=$((MISSING_COUNT + 1))
        if [[ $MISSING_COUNT -ge 10 ]]; then
            echo "   ... and more"
            break
        fi
    fi
done < <(find "$PROJECT_ROOT/frontend/lib" "$PROJECT_ROOT/frontend/components" -name "*.ts" -o -name "*.tsx" -print0 2>/dev/null || true)

if [[ $MISSING_COUNT -gt 0 ]]; then
    echo ""
    echo -e "${YELLOW}   Consider adding license headers to source files.${NC}"
    echo "   This is a recommendation, not blocking commit."
fi

if [[ $CHECKED_COUNT -eq 0 ]]; then
    echo -e "${GREEN}✓ No source files to check${NC}"
else
    echo -e "${GREEN}✓ Checked $CHECKED_COUNT files${NC}"
fi

# Don't fail - license headers are optional
exit 0
