# LinkWave Development Makefile
# Usage: make [target]

.PHONY: help install run run-all stop stop-all clean logs \
        docker-up docker-down docker-logs docker-ps \
        backend backend-build frontend frontend-build \
        db-reset test lint

# Colors for terminal output
CYAN := \033[36m
GREEN := \033[32m
YELLOW := \033[33m
RED := \033[31m
RESET := \033[0m

# Default target
.DEFAULT_GOAL := help

#==============================================================================
# HELP
#==============================================================================

help: ## Show this help message
	@echo ""
	@echo "$(CYAN)LinkWave Development Commands$(RESET)"
	@echo "=============================="
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(GREEN)%-18s$(RESET) %s\n", $$1, $$2}'
	@echo ""

#==============================================================================
# QUICK START
#==============================================================================

install: ## Install all dependencies (frontend + backend)
	@echo "$(CYAN)Installing frontend dependencies...$(RESET)"
	cd frontend && npm install
	@echo "$(CYAN)Building backend...$(RESET)"
	cd backend && ./gradlew build -x test
	@echo "$(GREEN)✓ All dependencies installed$(RESET)"

run: docker-up backend frontend ## Start everything (Docker + Backend + Frontend)
	@echo "$(GREEN)✓ All services started!$(RESET)"
	@echo ""
	@echo "$(CYAN)Services:$(RESET)"
	@echo "  Frontend:    http://localhost:3000"
	@echo "  Backend:     http://localhost:8080"
	@echo "  MailHog UI:  http://localhost:8025"
	@echo ""

run-all: run ## Alias for 'run'

stop: ## Stop backend and frontend (keeps Docker running)
	@echo "$(YELLOW)Stopping backend and frontend...$(RESET)"
	-@pkill -f "gradlew.*bootRun" 2>/dev/null || true
	-@pkill -f "next-server" 2>/dev/null || true
	-@pkill -f "next dev" 2>/dev/null || true
	@echo "$(GREEN)✓ Backend and frontend stopped$(RESET)"

stop-all: stop docker-down ## Stop everything including Docker
	@echo "$(GREEN)✓ All services stopped$(RESET)"

#==============================================================================
# DOCKER COMMANDS
#==============================================================================

docker-up: ## Start Docker services (PostgreSQL, Redis, Kafka, MailHog)
	@echo "$(CYAN)Starting Docker services...$(RESET)"
	docker-compose up -d
	@echo "$(CYAN)Waiting for services to be healthy...$(RESET)"
	@sleep 5
	@docker-compose ps
	@echo "$(GREEN)✓ Docker services started$(RESET)"

docker-down: ## Stop Docker services
	@echo "$(YELLOW)Stopping Docker services...$(RESET)"
	docker-compose down
	@echo "$(GREEN)✓ Docker services stopped$(RESET)"

docker-logs: ## Show Docker services logs
	docker-compose logs -f

docker-ps: ## Show Docker services status
	docker-compose ps

docker-clean: ## Stop Docker and remove volumes (WARNING: deletes data)
	@echo "$(RED)WARNING: This will delete all data!$(RESET)"
	@read -p "Are you sure? [y/N] " confirm && [ "$$confirm" = "y" ] || exit 1
	docker-compose down -v
	@echo "$(GREEN)✓ Docker services and volumes removed$(RESET)"

#==============================================================================
# BACKEND COMMANDS
#==============================================================================

backend: ## Start backend server (Spring Boot)
	@echo "$(CYAN)Starting backend server...$(RESET)"
	@echo "$(YELLOW)Backend will be available at http://localhost:8080$(RESET)"
	cd backend && SPRING_PROFILES_ACTIVE=local ./gradlew bootRun &
	@sleep 3
	@echo "$(GREEN)✓ Backend starting in background$(RESET)"

backend-fg: ## Start backend server in foreground
	@echo "$(CYAN)Starting backend server (foreground)...$(RESET)"
	cd backend && SPRING_PROFILES_ACTIVE=local ./gradlew bootRun

backend-build: ## Build backend
	@echo "$(CYAN)Building backend...$(RESET)"
	cd backend && ./gradlew build -x test
	@echo "$(GREEN)✓ Backend built$(RESET)"

backend-test: ## Run backend tests
	@echo "$(CYAN)Running backend tests...$(RESET)"
	cd backend && ./gradlew test
	@echo "$(GREEN)✓ Backend tests completed$(RESET)"

backend-clean: ## Clean backend build
	cd backend && ./gradlew clean

#==============================================================================
# FRONTEND COMMANDS
#==============================================================================

frontend: ## Start frontend server (Next.js)
	@echo "$(CYAN)Starting frontend server...$(RESET)"
	@echo "$(YELLOW)Frontend will be available at http://localhost:3000$(RESET)"
	cd frontend && npm run dev &
	@sleep 3
	@echo "$(GREEN)✓ Frontend starting in background$(RESET)"

frontend-fg: ## Start frontend server in foreground
	@echo "$(CYAN)Starting frontend server (foreground)...$(RESET)"
	cd frontend && npm run dev

frontend-build: ## Build frontend for production
	@echo "$(CYAN)Building frontend...$(RESET)"
	cd frontend && npm run build
	@echo "$(GREEN)✓ Frontend built$(RESET)"

frontend-lint: ## Lint frontend code
	cd frontend && npm run lint

#==============================================================================
# DATABASE COMMANDS
#==============================================================================

db-shell: ## Open PostgreSQL shell
	docker exec -it linkwave-postgres psql -U linkwave -d linkwave

db-reset: ## Reset database (WARNING: deletes data)
	@echo "$(RED)WARNING: This will delete all database data!$(RESET)"
	@read -p "Are you sure? [y/N] " confirm && [ "$$confirm" = "y" ] || exit 1
	docker-compose down postgres-db
	docker volume rm linkwave-postgres-data 2>/dev/null || true
	docker-compose up -d postgres-db
	@echo "$(GREEN)✓ Database reset$(RESET)"

redis-cli: ## Open Redis CLI
	docker exec -it linkwave-redis redis-cli -a changeme

#==============================================================================
# UTILITY COMMANDS
#==============================================================================

logs: ## Show all logs (backend + docker)
	@echo "$(CYAN)Showing Docker logs...$(RESET)"
	docker-compose logs -f --tail=50

clean: backend-clean ## Clean all build artifacts
	@echo "$(CYAN)Cleaning build artifacts...$(RESET)"
	rm -rf frontend/.next frontend/node_modules/.cache
	@echo "$(GREEN)✓ Clean complete$(RESET)"

status: docker-ps ## Show status of all services
	@echo ""
	@echo "$(CYAN)Process Status:$(RESET)"
	@ps aux | grep -E "(gradlew|next)" | grep -v grep || echo "  No backend/frontend processes running"

test: backend-test ## Run all tests

#==============================================================================
# DEVELOPMENT SHORTCUTS
#==============================================================================

dev: docker-up ## Start Docker services only (for running backend/frontend manually)
	@echo ""
	@echo "$(GREEN)✓ Docker services ready$(RESET)"
	@echo ""
	@echo "$(CYAN)Now run in separate terminals:$(RESET)"
	@echo "  Terminal 1: make backend-fg"
	@echo "  Terminal 2: make frontend-fg"
	@echo ""

otp-test: ## Test OTP endpoint
	@echo "$(CYAN)Testing OTP request...$(RESET)"
	curl -s -X POST http://localhost:8080/api/v1/auth/request-otp \
		-H "Content-Type: application/json" \
		-d '{"phoneNumber": "+919876543210", "email": "test@example.com"}' | jq .
	@echo ""
	@echo "$(YELLOW)Check backend console for OTP code$(RESET)"
