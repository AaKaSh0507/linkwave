#!/usr/bin/env bash
# =============================================================================
# LinkWave - Pre-commit Setup Script
# Installs and configures pre-commit hooks for the project
# =============================================================================

set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo ""
echo -e "${BOLD}${CYAN}╔═══════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}${CYAN}║           LinkWave - Pre-commit Setup                             ║${NC}"
echo -e "${BOLD}${CYAN}╚═══════════════════════════════════════════════════════════════════╝${NC}"
echo ""

# =============================================================================
# Helper Functions
# =============================================================================

log_info() {
  echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
  echo -e "${GREEN}✓ $1${NC}"
}

log_warning() {
  echo -e "${YELLOW}⚠️  $1${NC}"
}

log_error() {
  echo -e "${RED}❌ $1${NC}"
}

check_command() {
  if command -v "$1" &>/dev/null; then
    return 0
  else
    return 1
  fi
}

# =============================================================================
# Check Prerequisites
# =============================================================================

echo -e "${BOLD}Checking prerequisites...${NC}"
echo ""

MISSING_DEPS=0

# Check Python
if check_command python3; then
  PYTHON_VERSION=$(python3 --version 2>&1 | cut -d' ' -f2)
  log_success "Python: $PYTHON_VERSION"
else
  log_error "Python 3 not found"
  MISSING_DEPS=1
fi

# Check pip
if check_command pip3 || check_command pip; then
  # PIP_CMD=$(check_command pip3 && echo "pip3" || echo "pip")
  log_success "pip available"
else
  log_error "pip not found"
  MISSING_DEPS=1
fi

# Check Node.js
if check_command node; then
  NODE_VERSION=$(node --version)
  log_success "Node.js: $NODE_VERSION"
else
  log_warning "Node.js not found (needed for JS/TS hooks)"
fi

# Check npm
if check_command npm; then
  NPM_VERSION=$(npm --version)
  log_success "npm: $NPM_VERSION"
else
  log_warning "npm not found (needed for JS/TS hooks)"
fi

# Check Java
if check_command java; then
  JAVA_VERSION=$(java -version 2>&1 | head -1)
  log_success "Java: $JAVA_VERSION"
else
  log_warning "Java not found (needed for Java hooks)"
fi

# Check Git
if check_command git; then
  GIT_VERSION=$(git --version | cut -d' ' -f3)
  log_success "Git: $GIT_VERSION"
else
  log_error "Git not found"
  MISSING_DEPS=1
fi

echo ""

if [[ $MISSING_DEPS -eq 1 ]]; then
  log_error "Missing required dependencies. Please install them and retry."
  exit 1
fi

# =============================================================================
# Install pre-commit
# =============================================================================

echo -e "${BOLD}Installing pre-commit...${NC}"
echo ""

if check_command pre-commit; then
  PRECOMMIT_VERSION=$(pre-commit --version | cut -d' ' -f2)
  log_success "pre-commit already installed: $PRECOMMIT_VERSION"
else
  log_info "Installing pre-commit via pip..."
  pip3 install pre-commit --quiet
  log_success "pre-commit installed"
fi

# =============================================================================
# Install Git Hooks
# =============================================================================

echo ""
echo -e "${BOLD}Installing Git hooks...${NC}"
echo ""

cd "$PROJECT_ROOT"

# Install pre-commit hooks
log_info "Installing pre-commit hook..."
pre-commit install
log_success "pre-commit hook installed"

# Install commit-msg hook
log_info "Installing commit-msg hook..."
pre-commit install --hook-type commit-msg
log_success "commit-msg hook installed"

# =============================================================================
# Make Custom Hooks Executable
# =============================================================================

echo ""
echo -e "${BOLD}Configuring custom hooks...${NC}"
echo ""

if [[ -d "$PROJECT_ROOT/.pre-commit-hooks" ]]; then
  chmod +x "$PROJECT_ROOT/.pre-commit-hooks/"*.sh 2>/dev/null || true
  log_success "Custom hooks made executable"
fi

# =============================================================================
# Initialize secrets baseline (if detect-secrets is available)
# =============================================================================

echo ""
echo -e "${BOLD}Setting up secrets detection...${NC}"
echo ""

if check_command detect-secrets || pip3 show detect-secrets &>/dev/null; then
  if [[ ! -f "$PROJECT_ROOT/.secrets.baseline" ]]; then
    log_info "Creating secrets baseline..."
    detect-secrets scan --exclude-files '\.lock$' --exclude-files 'package-lock\.json$' >"$PROJECT_ROOT/.secrets.baseline" 2>/dev/null || true
    log_success "Secrets baseline created"
  else
    log_success "Secrets baseline already exists"
  fi
else
  log_warning "detect-secrets not installed, skipping baseline creation"
  log_info "Install with: pip install detect-secrets"
fi

# =============================================================================
# Install Node.js dependencies (for frontend hooks)
# =============================================================================

echo ""
echo -e "${BOLD}Setting up frontend hooks...${NC}"
echo ""

if [[ -d "$PROJECT_ROOT/frontend" ]] && check_command npm; then
  cd "$PROJECT_ROOT/frontend"

  # Check if node_modules exists
  if [[ ! -d "node_modules" ]]; then
    log_info "Installing frontend dependencies..."
    npm ci --silent 2>/dev/null || npm install --silent
    log_success "Frontend dependencies installed"
  else
    log_success "Frontend dependencies already installed"
  fi

  # Install husky if package.json has prepare script
  if grep -q '"prepare"' package.json 2>/dev/null; then
    log_info "Running npm prepare..."
    npm run prepare --silent 2>/dev/null || true
  fi

  cd "$PROJECT_ROOT"
fi

# =============================================================================
# Verify Installation
# =============================================================================

echo ""
echo -e "${BOLD}Verifying installation...${NC}"
echo ""

# Run pre-commit on a subset of files to verify
log_info "Running verification check..."
if pre-commit run --all-files --show-diff-on-failure 2>/dev/null; then
  log_success "All hooks passed verification"
else
  log_warning "Some hooks reported issues (this is expected for existing code)"
fi

# =============================================================================
# Summary
# =============================================================================

echo ""
echo -e "${BOLD}${GREEN}╔═══════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}${GREEN}║           Setup Complete!                                         ║${NC}"
echo -e "${BOLD}${GREEN}╚═══════════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BOLD}What's installed:${NC}"
echo "  • pre-commit hooks (run on every commit)"
echo "  • commit-msg hooks (validate commit messages)"
echo "  • custom project hooks"
echo ""
echo -e "${BOLD}Usage:${NC}"
echo "  ${CYAN}git commit${NC}                    - Hooks run automatically"
echo "  ${CYAN}pre-commit run --all-files${NC}   - Run all hooks on all files"
echo "  ${CYAN}pre-commit run <hook-id>${NC}     - Run specific hook"
echo "  ${CYAN}git commit --no-verify${NC}       - Skip hooks (emergency only!)"
echo ""
echo -e "${BOLD}Maintenance:${NC}"
echo "  ${CYAN}pre-commit autoupdate${NC}        - Update hooks to latest versions"
echo "  ${CYAN}pre-commit clean${NC}             - Clean hook environments"
echo ""
echo -e "${YELLOW}Note: First commit after setup may be slow while hooks are cached.${NC}"
echo ""
