# Linkwave Backend

Production-grade backend scaffolding for the Linkwave realtime chat application.

## Technology Stack

- **Java**: 21+ (toolchain configured for Java 21)
- **Spring Boot**: 3.4.1
- **Build Tool**: Gradle 8.12 with Kotlin DSL
- **Architecture**: MVC Layered (Controller → Service → Repository → Domain)
- **Database**: PostgreSQL (driver included, no entities yet)
- **Security**: Spring Security (session-based, no JWT yet)
- **Monitoring**: Spring Boot Actuator

## Dependencies Included

### Core Spring Boot Starters
- `spring-boot-starter-web` - REST API support
- `spring-boot-starter-validation` - Bean validation
- `spring-boot-starter-actuator` - Health checks and metrics
- `spring-boot-starter-security` - Authentication and authorization
- `spring-boot-starter-data-jpa` - Database access layer

### Database
- `postgresql` - PostgreSQL JDBC driver

### Development Tools
- `spring-boot-devtools` - Hot reload support
- `spring-boot-configuration-processor` - Configuration metadata

### Testing
- `spring-boot-starter-test` - JUnit 5, Mockito, AssertJ
- `spring-security-test` - Security testing utilities

## Project Structure

```
backend/
├── build.gradle.kts          # Gradle build configuration (Kotlin DSL)
├── settings.gradle.kts        # Gradle settings
├── Dockerfile                 # Multi-stage Docker build
├── src/
│   ├── main/
│   │   ├── java/com/linkwave/app/
│   │   │   ├── LinkwaveApplication.java  # Application entrypoint
│   │   │   ├── controller/               # REST controllers (future)
│   │   │   ├── service/                  # Business logic layer (future)
│   │   │   ├── repository/               # Data access layer (future)
│   │   │   ├── domain/                   # Domain entities (future)
│   │   │   └── config/                   # Configuration classes (future)
│   │   └── resources/
│   │       └── application.yml           # Application configuration
│   └── test/
│       └── java/com/linkwave/app/
│           └── PlaceholderTests.java     # Test scaffold
```

## Prerequisites

- **Java 21** or later
- **Gradle 8.x** (or use included wrapper)
- **PostgreSQL** (for database connectivity)
- **Docker** (for containerized builds)

### Java Version Management

If you have multiple Java versions installed (e.g., Java 21 and Java 25):

**Automatic (Recommended)**: Java 21 is now set by default in your shell
```bash
java -version  # Should show Java 21
```

**Helper Script**: Use the included script to switch Java versions
```bash
source use-java21.sh
```

**Manual Override**: Switch Java for a single terminal session
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk-21.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
```
The Docker build uses Java 21 internally and will work correctly.

## Build & Run Instructions

### Using Gradle

**Build the application:**
```bash
./gradlew build
```

**Run the application:**
```bash
./gradlew bootRun
```

**Run tests:**
```bash
./gradlew test
```

The application will start on `http://localhost:8080`

### Using Docker

**Build Docker image:**
```bash
docker build -t linkwave-backend:latest .
```

**Run Docker container:**
```bash
docker run -p 8080:8080 linkwave-backend:latest
```

## Configuration

Application configuration is located in `src/main/resources/application.yml`:

### Server Configuration
- Server port: 8080
- Session timeout: 30 minutes

### Database Configuration (Placeholder)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/linkwave
    username: linkwave
    password: changeme
```

**Note**: Update database credentials before running. No entities are implemented yet.

### Security Configuration
- Default user: `admin`
- Default password: `changeme`
- Session-based authentication (no JWT yet)

**Note**: Change default credentials in production.

### Actuator Endpoints
Available at `/actuator`:
- `/actuator/health` - Health check
- `/actuator/info` - Application info
- `/actuator/metrics` - Application metrics

## Development

This is a scaffold with dependencies configured for REST API development. The directory structure follows MVC layered architecture:

- **controller/**: REST API endpoints (ready for implementation)
- **service/**: Business logic and orchestration (ready for implementation)
- **repository/**: Database access and persistence (ready for implementation)
- **domain/**: Domain models and entities (ready for implementation)
- **config/**: Spring configuration classes (ready for implementation)

### Current Status
- ✅ All dependencies configured
- ✅ Database driver included (PostgreSQL)
- ✅ Security framework enabled (session-based)
- ✅ Actuator endpoints available
- ✅ JPA ready (no entities yet)
- ⏳ No REST controllers implemented
- ⏳ No business logic implemented
- ⏳ No database entities defined

## Next Steps

1. Implement domain entities in `domain/` package
2. Create repository interfaces for data access
3. Develop service layer business logic
4. Build REST controllers for API endpoints
5. Add database configuration and connectivity
6. Implement authentication and authorization
7. Add WebSocket support for realtime features

## License

Proprietary - Linkwave Project
