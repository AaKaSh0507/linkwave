# Linkwave Chat Application

![Build and Push](https://github.com/AaKaSh0507/linkwave/actions/workflows/build-and-push.yml/badge.svg)
![Pre-commit Checks](https://github.com/AaKaSh0507/linkwave/actions/workflows/pre-commit.yml/badge.svg)

A modern, real-time chat application built with Spring Boot and Next.js, featuring WebSocket-based messaging, presence indicators, typing indicators, and read receipts.

## 🏗️ Architecture

**Backend:**
- Spring Boot 3.x with Java 21
- WebSocket support for real-time messaging
- PostgreSQL database
- Redis for caching and pub/sub
- Gradle build system

**Frontend:**
- Next.js 15+ with React 19
- TypeScript
- TailwindCSS for styling
- ShadCN UI components
- WebSocket client

## 🚀 Quick Start

### Prerequisites

- Docker and Docker Compose
- Java 21 (for local development)
- Node.js 20+ (for local development)
- PostgreSQL 15+
- Redis 7+

### Running with Docker Compose

```bash
# Clone the repository
git clone https://github.com/AaKaSh0507/linkwave.git
cd linkwave

# Start all services (development mode)
docker-compose up -d

# Start all services (production mode)
docker-compose -f docker-compose.prod.yml up -d
```

The application will be available at:
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080
- WebSocket: ws://localhost:8080/ws

### Local Development

**Backend:**
```bash
cd backend
./gradlew bootRun
```

**Frontend:**
```bash
cd frontend
npm install
npm run dev
```

## 🐳 Docker Images

Pre-built Docker images are automatically published to GitHub Container Registry on every push to `main` and on version tags.

### Available Images

```bash
# Backend (Spring Boot)
ghcr.io/aakashmalik/linkwave-backend:latest
ghcr.io/aakashmalik/linkwave-backend:1.0.0
ghcr.io/aakashmalik/linkwave-backend:sha-a1b2c3d4

# Frontend (Next.js)
ghcr.io/aakashmalik/linkwave-frontend:latest
ghcr.io/aakashmalik/linkwave-frontend:1.0.0
ghcr.io/aakashmalik/linkwave-frontend:sha-a1b2c3d4
```

### Pulling Images

```bash
# Pull latest images
docker pull ghcr.io/aakashmalik/linkwave-backend:latest
docker pull ghcr.io/aakashmalik/linkwave-frontend:latest

# Pull specific version
docker pull ghcr.io/aakashmalik/linkwave-backend:1.0.0
docker pull ghcr.io/aakashmalik/linkwave-frontend:1.0.0
```

### Running Images Locally

```bash
# Backend
docker run -d \
  --name linkwave-backend \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/linkwave \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  -e SPRING_REDIS_HOST=host.docker.internal \
  ghcr.io/aakashmalik/linkwave-backend:latest

# Frontend
docker run -d \
  --name linkwave-frontend \
  -p 3000:3000 \
  -e NEXT_PUBLIC_API_URL=http://localhost:8080 \
  -e NEXT_PUBLIC_WS_URL=ws://localhost:8080/ws \
  ghcr.io/aakashmalik/linkwave-frontend:latest
```

## 🔄 CI/CD Pipeline

### Workflow Overview

The project uses GitHub Actions for automated building and publishing of Docker images.

**Triggers:**
- Push to `main` branch → Build and push `latest` tag
- Git tags (`v*.*.*`) → Build and push versioned releases
- Pull requests → Build only (validation, no push)
- Manual workflow dispatch → On-demand builds

**Build Jobs:**
- `build-backend` - Builds Spring Boot application (parallel)
- `build-frontend` - Builds Next.js application (parallel)
- `security-scan` - Scans images for vulnerabilities with Trivy

### Image Tagging Strategy

| Event | Tags Generated |
|-------|---------------|
| Push to `main` | `latest`, `sha-a1b2c3d4` |
| Tag `v1.2.3` | `1.2.3`, `1.2`, `1`, `latest`, `sha-a1b2c3d4` |
| PR #42 | `pr-42` (build only, not pushed) |

### Triggering Manual Builds

1. Navigate to **Actions** tab in GitHub
2. Select **Build and Push** workflow
3. Click **Run workflow**
4. Select branch and click **Run workflow**

### Required Secrets

The workflow uses GitHub Container Registry (GHCR) which automatically authenticates using `GITHUB_TOKEN`. No additional secrets are required.

**Optional Secrets** (for custom API/WS URLs in frontend builds):
- `NEXT_PUBLIC_API_URL` - Backend API URL (default: `http://localhost:8080`)
- `NEXT_PUBLIC_WS_URL` - WebSocket URL (default: `ws://localhost:8080/ws`)

### Build Optimizations

- **Docker Layer Caching**: Uses GitHub Actions cache to speed up builds
- **Gradle Caching**: Backend builds cache Gradle dependencies
- **NPM Caching**: Frontend builds cache NPM dependencies
- **Multi-stage Builds**: Optimized Dockerfiles for minimal image sizes
- **Parallel Jobs**: Backend and frontend build simultaneously

### Security Scanning

All pushed images are automatically scanned with [Trivy](https://github.com/aquasecurity/trivy) for:
- HIGH and CRITICAL vulnerabilities
- Known CVEs in dependencies
- Container misconfigurations

Scan results are:
- Uploaded to GitHub Security tab
- Available as workflow artifacts (retained for 7 days)
- Non-blocking (workflow succeeds even with vulnerabilities)

## 🧪 Testing

### Backend Tests

```bash
cd backend
./gradlew test
./gradlew integrationTest
```

### Frontend Tests

```bash
cd frontend
npm run test
npm run test:e2e
```

### Pre-commit Hooks

This project uses pre-commit hooks for code quality:

```bash
# Install pre-commit
pip install pre-commit

# Install hooks
pre-commit install

# Run manually
pre-commit run --all-files
```

## 📝 Development Workflow

### Making Changes

1. Create a feature branch from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. Make your changes and commit:
   ```bash
   git add .
   git commit -m "feat: your feature description"
   ```
   
   Follow [Conventional Commits](https://www.conventionalcommits.org/) format.

3. Push your branch:
   ```bash
   git push origin feature/your-feature-name
   ```

4. Open a Pull Request:
   - The CI workflow will build your changes (validation only)
   - Pre-commit checks will run automatically
   - Code review is required before merging

5. After merge to `main`:
   - Images are automatically built and published with `latest` tag
   - Available immediately in GHCR

### Creating Releases

1. Create and push a version tag:
   ```bash
   git tag -a v1.0.0 -m "Release version 1.0.0"
   git push origin v1.0.0
   ```

2. The workflow automatically:
   - Builds images with versioned tags (`1.0.0`, `1.0`, `1`)
   - Updates `latest` tag
   - Publishes to GHCR

### Skipping CI

Add `[skip ci]` or `[ci skip]` to your commit message to skip the build workflow:

```bash
git commit -m "docs: update README [skip ci]"
```

## 🛠️ Configuration

### Environment Variables

**Backend:**
```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/linkwave
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
```

**Frontend:**
```bash
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_WS_URL=ws://localhost:8080/ws
```

## 📦 Project Structure

```
linkwave/
├── backend/              # Spring Boot backend
│   ├── src/
│   ├── build.gradle.kts
│   └── ...
├── frontend/             # Next.js frontend
│   ├── app/
│   ├── components/
│   ├── lib/
│   └── package.json
├── docker/               # Dockerfiles
│   ├── api/
│   │   └── Dockerfile.prod
│   └── frontend/
│       └── Dockerfile.prod
├── .github/
│   └── workflows/
│       ├── build-and-push.yml
│       └── pre-commit.yml
├── docker-compose.yml
└── docker-compose.prod.yml
```

## 🤝 Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🔗 Links

- [GitHub Repository](https://github.com/AaKaSh0507/linkwave)
- [Container Registry](https://github.com/AaKaSh0507/linkwave/pkgs/container/linkwave-backend)
- [Issues](https://github.com/AaKaSh0507/linkwave/issues)
- [Pull Requests](https://github.com/AaKaSh0507/linkwave/pulls)

## 📞 Support

For support, please open an issue on GitHub or contact the maintainers.
