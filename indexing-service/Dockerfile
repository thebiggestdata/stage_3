# Multi-stage build for Indexing Service
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy parent pom first for caching
COPY pom.xml /build/pom.xml
RUN mvn -N install -q || true

# Copy service pom and download dependencies
COPY indexing-service/pom.xml /build/indexing-service/pom.xml
WORKDIR /build/indexing-service
RUN mvn dependency:go-offline -q || true

# Copy source and build
COPY indexing-service/src /build/indexing-service/src
RUN mvn package -DskipTests -q

# Runtime image
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /build/indexing-service/target/indexing-service-*.jar /app/app.jar

# Expose ports: HTTP API + Hazelcast
EXPOSE 7002 5702

ENTRYPOINT ["java", "-jar", "/app/app.jar"]