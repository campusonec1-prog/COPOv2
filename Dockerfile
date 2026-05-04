# =============================================================
# COPO - Production Dockerfile
# Multi-stage build: Builder → Runtime
# =============================================================

# -------------------------
# Stage 1: Build
# -------------------------
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Cache dependencies layer separately (faster rebuilds)
COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B --no-transfer-progress

# -------------------------
# Stage 2: Runtime (slim JRE image)
# -------------------------
FROM eclipse-temurin:17-jre-alpine
LABEL maintainer="COPO Project"
LABEL version="2.0.0"
LABEL description="Course Outcome Process Outcome - Production"

WORKDIR /app

# Create non-root user for security
RUN addgroup -S copogroup && adduser -S copouser -G copogroup

# Create logs directory with proper permissions
RUN mkdir -p /app/logs && chown -R copouser:copogroup /app/logs

# Copy built JAR from builder stage
COPY --from=build /app/target/*.jar app.jar
RUN chown copouser:copogroup app.jar

# Switch to non-root user
USER copouser

# Expose application port
EXPOSE 9091

# JVM tuning for containerized environments
ENV JAVA_OPTS="-server \
  -Xms256m \
  -Xmx512m \
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UseG1GC \
  -XX:+UseStringDeduplication \
  -Djava.security.egd=file:/dev/./urandom \
  -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT:-9091}/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
