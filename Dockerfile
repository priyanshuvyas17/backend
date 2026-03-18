# ============================================
# Stage 1: Build
# ============================================
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml first (better layer caching)
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Download dependencies (cached when pom unchanged; resolve works with custom repos)
RUN ./mvnw dependency:resolve dependency:resolve-plugins -B

# Copy source code
COPY src ./src

# Build JAR (skip tests for faster builds)
RUN ./mvnw package -DskipTests -B

# ============================================
# Stage 2: Runtime
# ============================================
FROM eclipse-temurin:17-jre-alpine

# Create non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

# Copy built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Change ownership
RUN chown -R appuser:appgroup /app

USER appuser

# Render sets PORT env var dynamically; Spring Boot reads it via server.port=${PORT:10000}
ENV PORT=10000
EXPOSE ${PORT}

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
