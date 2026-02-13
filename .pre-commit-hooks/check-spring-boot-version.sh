#!/usr/bin/env bash
# =============================================================================
# Check Spring Boot Version Consistency
# Ensures Spring Boot version is consistent across the project
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "🌱 Checking Spring Boot version consistency..."

# Expected version (update this when upgrading)
EXPECTED_VERSION="3.4.1"

# Check build.gradle.kts
if [[ -f "$PROJECT_ROOT/backend/build.gradle.kts" ]]; then
  GRADLE_VERSION=$(grep -oP 'org\.springframework\.boot.*version\s*"\K[^"]+' "$PROJECT_ROOT/backend/build.gradle.kts" 2>/dev/null || echo "")

  if [[ -z "$GRADLE_VERSION" ]]; then
    # Try alternate pattern for plugin version
    GRADLE_VERSION=$(grep -oP 'id\("org\.springframework\.boot"\)\s+version\s+"\K[^"]+' "$PROJECT_ROOT/backend/build.gradle.kts" 2>/dev/null || echo "")
  fi

  if [[ -n "$GRADLE_VERSION" ]]; then
    if [[ "$GRADLE_VERSION" != "$EXPECTED_VERSION" ]]; then
      echo -e "${YELLOW}⚠️  Spring Boot version mismatch:${NC}"
      echo -e "   Expected: ${GREEN}$EXPECTED_VERSION${NC}"
      echo -e "   Found:    ${RED}$GRADLE_VERSION${NC} (in build.gradle.kts)"
      echo ""
      echo "   Update EXPECTED_VERSION in this script after intentional upgrades."
      # Warning only, don't fail
      exit 0
    fi
    echo -e "${GREEN}✓ Spring Boot version: $GRADLE_VERSION${NC}"
  fi
fi

# Check pom.xml if exists
if [[ -f "$PROJECT_ROOT/backend/pom.xml" ]]; then
  MAVEN_VERSION=$(grep -oP '<spring-boot\.version>\K[^<]+' "$PROJECT_ROOT/backend/pom.xml" 2>/dev/null || echo "")

  if [[ -z "$MAVEN_VERSION" ]]; then
    MAVEN_VERSION=$(grep -A1 'spring-boot-starter-parent' "$PROJECT_ROOT/backend/pom.xml" | grep -oP '<version>\K[^<]+' 2>/dev/null || echo "")
  fi

  if [[ -n "$MAVEN_VERSION" && "$MAVEN_VERSION" != "$EXPECTED_VERSION" ]]; then
    echo -e "${YELLOW}⚠️  Spring Boot version mismatch in pom.xml:${NC}"
    echo -e "   Expected: ${GREEN}$EXPECTED_VERSION${NC}"
    echo -e "   Found:    ${RED}$MAVEN_VERSION${NC}"
    exit 0
  fi
fi

exit 0
