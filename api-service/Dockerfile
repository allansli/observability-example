# Multi-stage build for Java 21 application with virtual threads
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Set working directory
WORKDIR /app

# Copy parent pom and shared modules first for better layer caching
COPY pom.xml ./
COPY shared-models ./shared-models
COPY api-service/pom.xml ./api-service/

# Download dependencies
RUN mvn dependency:go-offline -pl api-service -am

# Copy source code
COPY api-service/src ./api-service/src

# Build the application
RUN mvn clean package -pl api-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Install curl for health checks
RUN apk add --no-cache curl

# Create application directory
WORKDIR /app

# Create logs directory
RUN mkdir -p /app/logs

# Copy JAR file
COPY --from=build /app/api-service/target/api-service-*.jar app.jar

# Create non-root user
RUN addgroup -g 1001 -S appuser && \
    adduser -S appuser -u 1001 -G appuser

# Change ownership of app directory
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/v1/orders/health || exit 1

# JVM options for optimal performance with virtual threads
ENV JAVA_OPTS="-XX:+UseG1GC \
               -XX:MaxGCPauseMillis=100 \
               -XX:+UseStringDeduplication \
               -Xms256m \
               -Xmx1g \
               -XX:+ExitOnOutOfMemoryError \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=docker"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]