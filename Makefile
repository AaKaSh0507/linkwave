# =============================================================================
# LinkWave - Makefile
# Production-grade commands for development and deployment
# =============================================================================

.PHONY: help dev prod build build-prod up down restart logs logs-api logs-frontend \
        shell-api shell-db shell-redis db-migrate db-seed db-backup db-restore \
        test clean clean-all status health ps k8s-deploy k8s-rollback

# Colors
CYAN := \033[36m
GREEN := \033[32m
YELLOW := \033[33m
RED := \033[31m
RESET := \033[0m
BOLD := \033[1m

# Configuration
COMPOSE_DEV := docker compose -f deployments/docker-compose.dev.yml
COMPOSE_PROD := docker compose -f deployments/docker-compose.prod.yml
BACKUP_DIR := ./backups

# Default target
.DEFAULT_GOAL := help

# =============================================================================
# HELP
# =============================================================================

help: ## Display this help message
	@echo ""
	@echo "$(BOLD)$(CYAN)╔═══════════════════════════════════════════════════════════════════╗$(RESET)"
	@echo "$(BOLD)$(CYAN)║           LinkWave - Docker Management Commands                  ║$(RESET)"
	@echo "$(BOLD)$(CYAN)╚═══════════════════════════════════════════════════════════════════╝$(RESET)"
	@echo ""
	@echo "$(BOLD)Usage:$(RESET) make [target]"
	@echo ""
	@echo "$(BOLD)$(GREEN)Development:$(RESET)"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | grep -E '(dev|build[^-]|up|down|restart|logs|shell|status|health|ps)' | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(CYAN)%-18s$(RESET) %s\n", $$1, $$2}'
	@echo ""
	@echo "$(BOLD)$(GREEN)Production:$(RESET)"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | grep -E '(prod|build-prod)' | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(CYAN)%-18s$(RESET) %s\n", $$1, $$2}'
	@echo ""
	@echo "$(BOLD)$(GREEN)Database:$(RESET)"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | grep -E '(db-)' | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(CYAN)%-18s$(RESET) %s\n", $$1, $$2}'
	@echo ""
	@echo "$(BOLD)$(GREEN)Maintenance:$(RESET)"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | grep -E '(clean|test)' | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(CYAN)%-18s$(RESET) %s\n", $$1, $$2}'
	@echo ""

# =============================================================================
# DEVELOPMENT
# =============================================================================

dev: ## Start development environment
	@echo "$(CYAN)🚀 Starting development environment...$(RESET)"
	$(COMPOSE_DEV) up -d
	@echo ""
	@echo "$(GREEN)✓ Development environment started!$(RESET)"
	@echo ""
	@echo "$(BOLD)Services:$(RESET)"
	@echo "  $(CYAN)Frontend:$(RESET)    http://localhost:3000"
	@echo "  $(CYAN)API:$(RESET)         http://localhost:8080"
	@echo "  $(CYAN)API Docs:$(RESET)    http://localhost:8080/docs"
	@echo "  $(CYAN)MailHog:$(RESET)     http://localhost:8025"
	@echo ""

build: ## Build all development images
	@echo "$(CYAN)🔨 Building development images...$(RESET)"
	$(COMPOSE_DEV) build
	@echo "$(GREEN)✓ Build complete!$(RESET)"

build-no-cache: ## Build images without cache
	@echo "$(CYAN)🔨 Building images (no cache)...$(RESET)"
	$(COMPOSE_DEV) build --no-cache
	@echo "$(GREEN)✓ Build complete!$(RESET)"

up: ## Start all services (detached)
	@echo "$(CYAN)🚀 Starting services...$(RESET)"
	$(COMPOSE_DEV) up -d
	@echo "$(GREEN)✓ Services started!$(RESET)"

down: ## Stop all services
	@echo "$(YELLOW)🛑 Stopping services...$(RESET)"
	$(COMPOSE_DEV) down
	@echo "$(GREEN)✓ Services stopped!$(RESET)"

restart: down up ## Restart all services

logs: ## Tail logs from all services
	$(COMPOSE_DEV) logs -f

logs-api: ## Tail API logs
	$(COMPOSE_DEV) logs -f api

logs-frontend: ## Tail frontend logs
	$(COMPOSE_DEV) logs -f frontend

logs-db: ## Tail database logs
	$(COMPOSE_DEV) logs -f postgres

logs-kafka: ## Tail Kafka logs
	$(COMPOSE_DEV) logs -f kafka

status: ps ## Show service status (alias for ps)

ps: ## Show running containers
	@echo "$(BOLD)$(CYAN)Service Status:$(RESET)"
	@echo ""
	$(COMPOSE_DEV) ps

health: ## Check health of all services
	@echo "$(BOLD)$(CYAN)🏥 Health Check:$(RESET)"
	@echo ""
	@echo "$(BOLD)API:$(RESET)"
	@curl -sf http://localhost:8080/actuator/health 2>/dev/null | python3 -m json.tool 2>/dev/null || echo "  $(RED)❌ Not reachable$(RESET)"
	@echo ""
	@echo "$(BOLD)Frontend:$(RESET)"
	@curl -sf -o /dev/null -w "  Status: HTTP %{http_code}\n" http://localhost:3000 2>/dev/null || echo "  $(RED)❌ Not reachable$(RESET)"
	@echo ""
	@echo "$(BOLD)Database:$(RESET)"
	@docker exec linkwave-postgres pg_isready -U linkwave -d linkwave 2>/dev/null && echo "  $(GREEN)✓ Healthy$(RESET)" || echo "  $(RED)❌ Not ready$(RESET)"
	@echo ""
	@echo "$(BOLD)Redis:$(RESET)"
	@docker exec linkwave-redis redis-cli -a linkwave_dev ping 2>/dev/null | grep -q PONG && echo "  $(GREEN)✓ Healthy$(RESET)" || echo "  $(RED)❌ Not ready$(RESET)"
	@echo ""

# =============================================================================
# PRODUCTION
# =============================================================================

prod: ## Start production environment
	@echo "$(CYAN)🚀 Starting production environment...$(RESET)"
	$(COMPOSE_PROD) up -d
	@echo "$(GREEN)✓ Production environment started!$(RESET)"

prod-build: ## Build production images
	@echo "$(CYAN)🔨 Building production images...$(RESET)"
	$(COMPOSE_PROD) build
	@echo "$(GREEN)✓ Production build complete!$(RESET)"

prod-down: ## Stop production environment
	@echo "$(YELLOW)🛑 Stopping production environment...$(RESET)"
	$(COMPOSE_PROD) down
	@echo "$(GREEN)✓ Production stopped!$(RESET)"

prod-logs: ## Tail production logs
	$(COMPOSE_PROD) logs -f

prod-ps: ## Show production containers
	$(COMPOSE_PROD) ps

# =============================================================================
# SHELL ACCESS
# =============================================================================

shell-api: ## Access API container shell
	@echo "$(CYAN)🐚 Connecting to API container...$(RESET)"
	docker exec -it linkwave-api sh

shell-db: ## Access database shell (psql)
	@echo "$(CYAN)🐘 Connecting to PostgreSQL...$(RESET)"
	docker exec -it linkwave-postgres psql -U linkwave -d linkwave

shell-redis: ## Access Redis CLI
	@echo "$(CYAN)📮 Connecting to Redis...$(RESET)"
	docker exec -it linkwave-redis redis-cli -a linkwave_dev

shell-kafka: ## Access Kafka container shell
	@echo "$(CYAN)📨 Connecting to Kafka...$(RESET)"
	docker exec -it linkwave-kafka bash

shell-frontend: ## Access frontend container shell
	@echo "$(CYAN)🎨 Connecting to frontend container...$(RESET)"
	docker exec -it linkwave-frontend sh

# =============================================================================
# DATABASE
# =============================================================================

db-migrate: ## Run database migrations (via JPA auto-ddl)
	@echo "$(CYAN)📦 Running database migrations...$(RESET)"
	@echo "$(YELLOW)Note: Spring Boot JPA handles migrations automatically on startup$(RESET)"
	$(COMPOSE_DEV) restart api

db-seed: ## Seed database with test data
	@echo "$(CYAN)🌱 Seeding database...$(RESET)"
	@echo "$(YELLOW)Note: Add seed scripts to docker/db/init-scripts/$(RESET)"

db-backup: ## Backup database
	@echo "$(CYAN)💾 Backing up database...$(RESET)"
	@mkdir -p $(BACKUP_DIR)/postgres
	@docker exec linkwave-postgres pg_dump -U linkwave -d linkwave > $(BACKUP_DIR)/postgres/backup_$$(date +%Y%m%d_%H%M%S).sql
	@echo "$(GREEN)✓ Backup saved to $(BACKUP_DIR)/postgres/$(RESET)"

db-restore: ## Restore database from backup (usage: make db-restore FILE=backup.sql)
	@if [ -z "$(FILE)" ]; then \
		echo "$(RED)Error: Please specify FILE=<backup_file>$(RESET)"; \
		echo "$(YELLOW)Usage: make db-restore FILE=backups/postgres/backup.sql$(RESET)"; \
		exit 1; \
	fi
	@echo "$(CYAN)📥 Restoring database from $(FILE)...$(RESET)"
	@docker exec -i linkwave-postgres psql -U linkwave -d linkwave < $(FILE)
	@echo "$(GREEN)✓ Database restored!$(RESET)"

db-reset: ## Reset database (WARNING: destroys data)
	@echo "$(RED)⚠️  WARNING: This will destroy all database data!$(RESET)"
	@read -p "Are you sure? [y/N] " confirm && [ "$$confirm" = "y" ] || exit 1
	$(COMPOSE_DEV) down -v postgres
	$(COMPOSE_DEV) up -d postgres
	@echo "$(GREEN)✓ Database reset!$(RESET)"

# =============================================================================
# TESTING
# =============================================================================

test: ## Run tests in containers
	@echo "$(CYAN)🧪 Running tests...$(RESET)"
	cd backend && ./gradlew test
	@echo "$(GREEN)✓ Tests complete!$(RESET)"

test-api: ## Run API tests
	@echo "$(CYAN)🧪 Running API tests...$(RESET)"
	cd backend && ./gradlew test

test-frontend: ## Run frontend tests
	@echo "$(CYAN)🧪 Running frontend tests...$(RESET)"
	cd frontend && npm test

lint: ## Run linters
	@echo "$(CYAN)🔍 Running linters...$(RESET)"
	cd frontend && npm run lint

# =============================================================================
# CLEANUP
# =============================================================================

clean: ## Stop and remove containers
	@echo "$(YELLOW)🧹 Cleaning up containers...$(RESET)"
	$(COMPOSE_DEV) down --remove-orphans
	$(COMPOSE_PROD) down --remove-orphans 2>/dev/null || true
	@echo "$(GREEN)✓ Cleanup complete!$(RESET)"

clean-volumes: ## Remove containers and volumes (WARNING: destroys data)
	@echo "$(RED)⚠️  WARNING: This will destroy all data!$(RESET)"
	@read -p "Are you sure? [y/N] " confirm && [ "$$confirm" = "y" ] || exit 1
	$(COMPOSE_DEV) down -v --remove-orphans
	$(COMPOSE_PROD) down -v --remove-orphans 2>/dev/null || true
	@echo "$(GREEN)✓ Volumes removed!$(RESET)"

clean-images: ## Remove project images
	@echo "$(YELLOW)🧹 Removing project images...$(RESET)"
	docker images | grep linkwave | awk '{print $$3}' | xargs -r docker rmi -f
	@echo "$(GREEN)✓ Images removed!$(RESET)"

clean-all: clean-volumes clean-images ## Full cleanup (containers, volumes, images)
	@echo "$(GREEN)✓ Full cleanup complete!$(RESET)"

prune: ## Docker system prune (free disk space)
	@echo "$(YELLOW)🧹 Pruning Docker system...$(RESET)"
	docker system prune -f
	@echo "$(GREEN)✓ Prune complete!$(RESET)"

# =============================================================================
# PRE-COMMIT HOOKS
# =============================================================================

.PHONY: pre-commit-install pre-commit-run pre-commit-update pre-commit-clean lint

pre-commit-install: ## Install pre-commit hooks
	@echo "$(CYAN)🔧 Installing pre-commit hooks...$(RESET)"
	@if command -v pre-commit >/dev/null 2>&1; then \
		pre-commit install; \
		pre-commit install --hook-type commit-msg; \
		echo "$(GREEN)✓ Pre-commit hooks installed!$(RESET)"; \
	else \
		echo "$(YELLOW)Installing pre-commit...$(RESET)"; \
		pip install pre-commit; \
		pre-commit install; \
		pre-commit install --hook-type commit-msg; \
		echo "$(GREEN)✓ Pre-commit installed and hooks configured!$(RESET)"; \
	fi

pre-commit-run: ## Run all pre-commit hooks on all files
	@echo "$(CYAN)🔍 Running pre-commit hooks on all files...$(RESET)"
	pre-commit run --all-files --show-diff-on-failure
	@echo "$(GREEN)✓ Pre-commit checks complete!$(RESET)"

pre-commit-update: ## Update pre-commit hooks to latest versions
	@echo "$(CYAN)⬆️  Updating pre-commit hooks...$(RESET)"
	pre-commit autoupdate
	@echo "$(GREEN)✓ Hooks updated!$(RESET)"

pre-commit-clean: ## Clean pre-commit cache
	@echo "$(YELLOW)🧹 Cleaning pre-commit cache...$(RESET)"
	pre-commit clean
	pre-commit gc
	@echo "$(GREEN)✓ Cache cleaned!$(RESET)"

lint: pre-commit-run ## Alias for pre-commit-run

lint-java: ## Run Java-specific linting
	@echo "$(CYAN)☕ Running Java linting...$(RESET)"
	@cd backend && ./gradlew checkstyleMain --daemon 2>/dev/null || true
	@echo "$(GREEN)✓ Java linting complete!$(RESET)"

lint-frontend: ## Run frontend linting
	@echo "$(CYAN)📘 Running frontend linting...$(RESET)"
	@cd frontend && npm run lint
	@echo "$(GREEN)✓ Frontend linting complete!$(RESET)"

format: ## Format all code
	@echo "$(CYAN)✨ Formatting all code...$(RESET)"
	@pre-commit run pretty-format-java --all-files || true
	@pre-commit run prettier --all-files || true
	@echo "$(GREEN)✓ Formatting complete!$(RESET)"

security-scan: ## Run security scans
	@echo "$(CYAN)🔒 Running security scans...$(RESET)"
	@pre-commit run gitleaks --all-files || true
	@pre-commit run detect-secrets --all-files || true
	@echo "$(GREEN)✓ Security scan complete!$(RESET)"

# =============================================================================
# UTILITIES
# =============================================================================

env: ## Copy .env.example to .env
	@if [ -f .env ]; then \
		echo "$(YELLOW)⚠️  .env already exists. Skipping...$(RESET)"; \
	else \
		cp config/env/.env.example .env; \
		echo "$(GREEN)✓ Created .env from .env.example$(RESET)"; \
		echo "$(YELLOW)Please edit .env with your configuration$(RESET)"; \
	fi

setup: env build pre-commit-install ## Initial setup (create .env, build, and install hooks)
	@docker volume ls | grep linkwave || echo "  No volumes created yet"
	@echo ""

# =============================================================================
# KUBERNETES
# =============================================================================

k8s-deploy: ## Deploy to Kubernetes cluster
	@echo "$(CYAN)☸️  Deploying to Kubernetes...$(RESET)"
	./scripts/k8s-deploy.sh

k8s-rollback: ## Rollback Kubernetes deployments
	@echo "$(CYAN)⏪ Rolling back Kubernetes deployments...$(RESET)"
	./scripts/k8s-rollback.sh
