# Multi-stage build for Ingestion Service
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy parent pom first for caching
COPY pom.xml /build/pom.xml
RUN mvn -N install -q || true

# Copy service pom and download dependencies
COPY ingestion-service/pom.xml /build/ingestion-service/pom.xml
WORKDIR /build/ingestion-service
RUN mvn dependency:go-offline -q || true

# Copy source and build
COPY ingestion-service/src /build/ingestion-service/src
RUN mvn package -DskipTests -q

# Runtime image
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /build/ingestion-service/target/ingestion-service-*.jar /app/app.jar

# Expose ports: HTTP API + Hazelcast
EXPOSE 7001 5701

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
