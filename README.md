# Linkwave - Production-Grade Realtime Chat Application

This repository contains the complete scaffolding for a production-grade realtime chat application named "Linkwave". The project is organized into two main components: backend and frontend.

## Repository Structure

```
linkwave/
├── backend/              # Java Spring Boot backend
└── frontend/             # Vue 3 + Vite + Vuetify frontend
```

## Quick Start

### Start Docker Services (Required First)

```bash
# Start all services (PostgreSQL, Redis, Kafka, Mailhog)
docker compose up -d

# Verify services are healthy
docker compose ps
```

See [DOCKER_SETUP.md](DOCKER_SETUP.md) for detailed Docker environment documentation.

### Backend (Java + Spring Boot)

```bash
cd backend
./gradlew bootRun
```

Server will start on `http://localhost:8080`

**Full instructions**: See [backend/README.md](backend/README.md)

### Frontend (Vue 3 + Vite + Vuetify)

```bash
cd frontend
npm install
npm run dev
```

Application will start on `http://localhost:3000`

**Full instructions**: See [frontend/README.md](frontend/README.md)

## CI/CD Pipeline

Automated continuous integration and deployment workflows using GitHub Actions.

### Workflows

**Backend CI/CD** ([.github/workflows/backend-ci.yaml](.github/workflows/backend-ci.yaml)):
- **Trigger**: Pull requests and pushes to `main` affecting `backend/` directory
- **Build & Test Job**:
  - Set up Java 21 (Temurin distribution)
  - Cache Gradle dependencies
  - Run `./gradlew build` and `./gradlew test`
  - Generate JaCoCo test coverage report
  - Upload build artifacts (JAR files)
- **Build & Publish Job** (main branch only):
  - Build multi-stage Docker image from [backend/Dockerfile](backend/Dockerfile)
  - Push to GitHub Container Registry with tags:
    - `latest` (main branch)
    - `main-<commit-sha>` (7-character short SHA)
  - Cache Docker layers for faster builds

**Frontend CI/CD** ([.github/workflows/frontend-ci.yaml](.github/workflows/frontend-ci.yaml)):
- **Trigger**: Pull requests and pushes to `main` affecting `frontend/` directory
- **Build & Test Job**:
  - Set up Node.js 20 LTS
  - Cache npm dependencies
  - Run `npm ci` for clean install
  - Run `npm run build` (Vite production build)
  - Upload build artifacts (dist/ directory)
- **Build & Publish Job** (main branch only):
  - Build production Nginx image from [frontend/Dockerfile](frontend/Dockerfile)
  - Push to GitHub Container Registry with tags:
    - `latest` (main branch)
    - `main-<commit-sha>` (7-character short SHA)
  - Cache Docker layers for faster builds

### Container Registry

**Images published to GitHub Container Registry (GHCR)**:
- Backend: `ghcr.io/aakashk0507/linkwave-backend:latest`
- Frontend: `ghcr.io/aakashk0507/linkwave-frontend:latest`

**Image naming convention**:
- REST-friendly lowercase naming
- Semantic tagging strategy: `latest` for production, commit SHA for traceability
- Platform: `linux/amd64`

### Authentication

GitHub Actions uses `GITHUB_TOKEN` for GHCR authentication:
- **Permissions**: Automatically granted `packages: write` permission
- **Scope**: Repository-scoped, expires after workflow completes
- **Security**: No manual secrets required; token injected by GitHub

### Pulling Images

**Public images** (after first push):
```bash
# Backend
docker pull ghcr.io/aakashk0507/linkwave-backend:latest

# Frontend
docker pull ghcr.io/aakashk0507/linkwave-frontend:latest
```

**Authenticated pull** (for private repositories):
```bash
# Create personal access token with read:packages scope
echo $GITHUB_PAT | docker login ghcr.io -u aakashk0507 --password-stdin

# Pull images
docker pull ghcr.io/aakashk0507/linkwave-backend:latest
```

### Workflow Status

Check build status:
- Visit [Actions tab](https://github.com/AaKaSh0507/linkwave/actions)
- Backend CI: ![Backend CI](https://github.com/AaKaSh0507/linkwave/actions/workflows/backend-ci.yaml/badge.svg)
- Frontend CI: ![Frontend CI](https://github.com/AaKaSh0507/linkwave/actions/workflows/frontend-ci.yaml/badge.svg)

### Future Enhancements

Planned CI/CD improvements (not yet implemented):
- Automated deployment to Kubernetes cluster
- Helm chart versioning and publishing
- Integration testing with Docker Compose
- Security scanning (Trivy, Snyk)
- Release automation with semantic versioning
- Multi-environment deployments (dev, staging, production)

## Technology Stack

### Backend
- **Language**: Java 21
- **Framework**: Spring Boot 3.4.1
- **Build Tool**: Gradle 8.12 (Kotlin DSL)
- **Architecture**: MVC Layered (Controller → Service → Repository → Domain)
- **Database**: PostgreSQL (driver configured)
- **Security**: Spring Security (session-based)
- **Monitoring**: Spring Boot Actuator
- **Testing**: JUnit 5

### Frontend
- **Framework**: Vue 3.5.13
- **Build Tool**: Vite 5.4.11
- **UI Library**: Vuetify 3.7.5 (Material Design)
- **State Management**: Pinia 2.2.8
- **Icons**: Material Design Icons

## Architecture

### Backend Layers
```
com.linkwave.app/
├── controller/     # REST API endpoints
├── service/        # Business logic layer
├── repository/     # Data access layer
├── domain/         # Domain models
└── config/         # Configuration classes
```

### Frontend Structure
```
src/
├── views/          # Page components (LoginView, ChatView)
├── components/     # Reusable UI components
├── store/          # Pinia state management
├── utils/          # Utility functions
└── assets/         # Static assets
```

## Features Scaffolded

### Backend
- ✅ Spring Boot application entrypoint
- ✅ MVC layered directory structure
- ✅ Gradle build configuration (Groovy DSL)
- ✅ JUnit test structure
- ✅ Multi-stage Dockerfile (JDK build → JRE runtime)
- ✅ Application configuration (YAML)

### Frontend
- ✅ Vue 3 application setup with Vuetify
- ✅ Pinia state management integration
- ✅ Login view component
- ✅ Chat view component with responsive layout
- ✅ Vite build configuration
- ✅ Nginx-based production Dockerfile
- ✅ API proxy configuration

## Docker Support

### Build and Run Backend
```bash
cd backend
docker build -t linkwave-backend .
docker run -p 8080:8080 linkwave-backend
```

### Build and Run Frontend
```bash
cd frontend
docker build -t linkwave-frontend .
docker run -p 80:80 linkwave-frontend
```

## Development Workflow

1. **Start Backend**: `cd backend && ./gradlew bootRun`
2. **Start Frontend**: `cd frontend && npm run dev`
3. **Access Application**: Navigate to `http://localhost:3000`
4. **API Proxy**: Frontend dev server proxies `/api` to backend at `http://localhost:8080`

## Testing

### Backend
```bash
cd backend
./gradlew test
```

### Frontend
```bash
cd frontend
npm run build  # Validates build configuration
```

## Important Notes

## Docker Development Environment

Complete local development environment using Docker Compose:

### Services Provided
- **postgres-db**: PostgreSQL 17.2 (primary database)
- **redis-cache**: Redis 7.4.2 (sessions & caching)
- **kafka-broker**: Kafka 7.8.0 (event streaming)
- **kafka-zookeeper**: Zookeeper 7.8.0 (Kafka coordination)
- **smtp-mailhog**: Mailhog 1.0.1 (SMTP mock server)

### Quick Commands
```bash
# Start services
docker compose up -d

# Check status
docker compose ps

# View logs
docker compose logs -f

# Stop services
docker compose down
```

**Note**: Spring Boot is not yet configured to connect to these services. This is environment scaffolding only.

For complete Docker setup documentation, see [DOCKER_SETUP.md](DOCKER_SETUP.md).

## Kubernetes Production Deployment

Production-grade Kubernetes cluster setup with k3s, Traefik, and Let's Encrypt TLS:

### Infrastructure Components
- **k3s**: Lightweight Kubernetes distribution for single-node or multi-node clusters
- **Traefik v2**: Modern HTTP reverse proxy and ingress controller (included with k3s)
- **cert-manager**: Automated TLS certificate management with Let's Encrypt
- **Namespace**: Dedicated `linkwave` namespace for application isolation

### Kubernetes Manifests
```
k8s/
├── namespace.yaml                      # Linkwave production namespace
├── letsencrypt-staging-issuer.yaml     # Let's Encrypt staging (testing)
├── letsencrypt-prod-issuer.yaml        # Let's Encrypt production (validated)
└── ingress-examples.yaml               # Ingress templates for REST API, Frontend, WebSocket
```

### Quick Deployment

**Prerequisites**:
- Ubuntu 22.04 LTS VPS with public IP
- Domain name with DNS management access
- 2 CPU cores, 4GB RAM, 40GB disk minimum

**Setup Steps**:
```bash
# 1. Install k3s with Traefik
curl -sfL https://get.k3s.io | sh -s - --write-kubeconfig-mode 644

# 2. Install cert-manager
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.14.1/cert-manager.yaml

# 3. Create namespace
kubectl apply -f k8s/namespace.yaml

# 4. Configure DNS A records
# Point your domains to VPS IP:
# - linkwave.example.com → your-server-ip
# - api.linkwave.example.com → your-server-ip
# - ws.linkwave.example.com → your-server-ip

# 5. Deploy Let's Encrypt issuers
kubectl apply -f k8s/letsencrypt-staging-issuer.yaml
kubectl apply -f k8s/letsencrypt-prod-issuer.yaml

# 6. Deploy ingress resources (after app workloads)
kubectl apply -f k8s/ingress-examples.yaml
```

**Complete Setup Guide**: See [CLUSTER_SETUP.md](CLUSTER_SETUP.md) for comprehensive instructions including:
- VPS provider recommendations (Hetzner, DigitalOcean, Linode)
- Server initial setup and firewall configuration
- k3s installation and verification
- cert-manager installation
- DNS configuration examples
- TLS certificate testing procedures
- Troubleshooting common issues
- Cluster management commands

### Ingress Routes

| Service | Domain | Purpose |
|---------|--------|---------|
| Frontend | `https://linkwave.example.com` | Vue.js static assets |
| Backend API | `https://api.linkwave.example.com` | Spring Boot REST API |
| WebSocket | `wss://ws.linkwave.example.com/ws` | Realtime chat connections |

**Note**: Replace `example.com` with your actual domain in all manifest files before deployment.

## Java Version Management
- Java 21 is required for building the backend
- Java 21 and Java 25 can coexist on the same system
- The project is configured to use Java 21 by default in your shell
- Use `source backend/use-java21.sh` to switch Java versions if needed

### No Business Logic Implemented
This is a **scaffold-only** project with zero business logic implementation. It provides:
- Directory structures following best practices
- Build configurations for both repositories
- Docker support for containerized deployments
- Placeholder views and components
- Development and production-ready configurations

### Future Implementation Phases
The following features are planned but not yet implemented:
- WebSocket support for realtime messaging
- Database integration and persistence
- Authentication and authorization
- User management
- Message queuing systems
- Redis caching
- API endpoints for chat functionality

## Project Goals

- **Production-Ready**: All configurations and structures follow production best practices
- **Scalable Architecture**: MVC layered backend, component-based frontend
- **Modern Stack**: Latest stable versions of all technologies
- **Docker-First**: Full containerization support for deployment
- **Developer-Friendly**: Clear structure, comprehensive READMEs, build automation

## License

Proprietary - Linkwave Project

## Support

For detailed instructions on each component, refer to the respective README files:
- [Backend Documentation](backend/README.md)
- [Frontend Documentation](frontend/README.md)
- [Docker Setup Guide](DOCKER_SETUP.md)
- [Kubernetes Cluster Setup](CLUSTER_SETUP.md)
