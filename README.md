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
