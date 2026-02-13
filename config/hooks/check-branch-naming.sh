#!/usr/bin/env bash
# =============================================================================
# Check Branch Naming Convention
# Enforces consistent branch naming across the team
# =============================================================================

set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Get current branch name
BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "")

if [[ -z "$BRANCH" ]]; then
  echo -e "${YELLOW}⚠️  Could not determine branch name${NC}"
  exit 0
fi

echo "🌿 Checking branch naming convention..."

# Skip for main/master/develop branches
if [[ "$BRANCH" =~ ^(main|master|develop|development|staging|release/.*)$ ]]; then
  echo -e "${GREEN}✓ Branch '$BRANCH' is a protected/release branch${NC}"
  exit 0
fi

# Branch naming pattern:
# - feature/TICKET-123-description
# - bugfix/TICKET-123-description
# - hotfix/TICKET-123-description
# - chore/description
# - docs/description
# - refactor/description
# - test/description
# - ci/description
# - release/v1.2.3
VALID_PATTERN="^(feature|bugfix|hotfix|fix|chore|docs|refactor|test|ci|perf|style|build)\/[a-z0-9._-]+$"

# More flexible pattern for feature branches with ticket numbers
TICKET_PATTERN="^(feature|bugfix|hotfix|fix)\/[A-Z]+-[0-9]+-[a-z0-9-]+$"

if [[ "$BRANCH" =~ $VALID_PATTERN ]] || [[ "$BRANCH" =~ $TICKET_PATTERN ]]; then
  echo -e "${GREEN}✓ Branch name '$BRANCH' follows convention${NC}"
  exit 0
fi

# Show warning but don't fail (to not block developers)
echo -e "${YELLOW}⚠️  Branch name doesn't follow naming convention:${NC}"
echo -e "   Current: ${RED}$BRANCH${NC}"
echo ""
echo -e "${CYAN}Expected formats:${NC}"
echo "   feature/TICKET-123-description"
echo "   bugfix/TICKET-123-description"
echo "   hotfix/TICKET-123-description"
echo "   chore/some-task"
echo "   docs/update-readme"
echo "   refactor/cleanup-code"
echo ""
echo -e "${CYAN}Examples:${NC}"
echo "   feature/LW-42-user-authentication"
echo "   bugfix/LW-123-fix-login-error"
echo "   chore/update-dependencies"
echo ""

# Exit with 0 to not block commits (change to 1 for strict enforcement)
exit 0
