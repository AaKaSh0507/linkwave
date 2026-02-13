# syntax=docker/dockerfile:1.4

# =============================================================================
# LINKWAVE API - Development Dockerfile
# Spring Boot 3.x with Java 21 - Multi-stage build
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1: Build
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Install required build tools
RUN apk add --no-cache bash

# Copy Gradle wrapper and build files first (layer caching)
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

# Make gradlew executable
RUN chmod +x gradlew

# Download dependencies (cached layer)
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew dependencies --no-daemon

# Copy source code
COPY src src

# Build the application with layered JAR
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew build -x test -x checkstyleMain -x checkstyleTest -x checkstyleGatling --no-daemon && \
    java -Djarmode=layertools -jar build/libs/*.jar extract --destination build/extracted

# -----------------------------------------------------------------------------
# Stage 2: Runtime
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

# Create non-root user for security
RUN addgroup -g 1001 -S linkwave && \
    adduser -u 1001 -S linkwave -G linkwave

WORKDIR /app

# Install runtime dependencies
RUN apk add --no-cache curl

# Copy layered application (optimizes Docker layer caching)
COPY --from=builder --chown=linkwave:linkwave /app/build/extracted/dependencies/ ./
COPY --from=builder --chown=linkwave:linkwave /app/build/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=linkwave:linkwave /app/build/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=linkwave:linkwave /app/build/extracted/application/ ./

# Switch to non-root user
USER linkwave

# JVM tuning for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -sf http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

# Use exec form for proper signal handling (SIGTERM)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
