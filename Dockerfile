# ── Stage 1: Build Jar ────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /workspace

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build application with static frontend bundled inside the JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Stage 2: Runtime Image ────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create unprivileged non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy compiled JAR from build stage
COPY --from=builder /workspace/target/leaseflow-*.jar app.jar
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]

