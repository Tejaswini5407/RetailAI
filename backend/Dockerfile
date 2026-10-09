# Stage 1: Build stage using Maven and Java 21
FROM maven:3.9.8-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy pom.xml for dependency resolution layer caching
COPY pom.xml .

# Download dependencies (go-offline) to cache layer
RUN mvn dependency:go-offline -B

# Copy application source code
COPY src ./src

# Package application executable JAR (skipping unit tests during Docker build)
RUN mvn clean package -DskipTests

# Stage 2: Runtime stage using lightweight Java 21 JRE
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Create non-root system group and user for security compliance
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy packaged JAR artifact from builder stage
COPY --from=builder /app/target/retailai-backend-0.0.1-SNAPSHOT.jar app.jar

# Set ownership to runtime user
RUN chown -R appuser:appgroup /app

# Switch to non-root execution user
USER appuser

# Expose default HTTP port
EXPOSE 8080

# Environment variable defaults for JVM memory and dynamic port binding
ENV PORT=8080
ENV JAVA_OPTS="-Xms256m -Xmx384m"

# Execute Spring Boot application
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
