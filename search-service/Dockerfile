# Multi-stage build for Search Service
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy parent pom first for caching
COPY pom.xml /build/pom.xml
RUN mvn -N install -q || true

# Copy service pom and download dependencies
COPY search-service/pom.xml /build/search-service/pom.xml
WORKDIR /build/search-service
RUN mvn dependency:go-offline -q || true

# Copy source and build
COPY search-service/src /build/search-service/src
RUN mvn package -DskipTests -q

# Runtime image
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /build/search-service/target/search-service-*.jar /app/app.jar

# Expose ports: HTTP API + Hazelcast
EXPOSE 7003 5703

ENTRYPOINT ["java", "-jar", "/app/app.jar"]


