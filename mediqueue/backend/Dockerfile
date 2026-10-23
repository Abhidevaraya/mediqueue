# ==============================================================================
# MediQueue — Multi-Stage Dockerfile (Java 21 / Spring Boot 3)
# ==============================================================================

# Stage 1: Builder
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace

# Copy maven wrapper and pom.xml first for efficient dependency layer caching
COPY mvnw pom.xml ./
COPY .mvn .mvn

# Download dependencies
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

# Copy source code and build production jar
COPY src src
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Create unprivileged application user
RUN groupadd -r mediqueue && useradd -r -g mediqueue -s /bin/false mediqueue

# Copy repackaged application jar from builder
COPY --from=builder /workspace/target/mediqueue-backend-0.0.1-SNAPSHOT.jar app.jar

# Setup local storage directory and permissions
RUN mkdir -p /app/storage/documents && chown -R mediqueue:mediqueue /app

USER mediqueue

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
