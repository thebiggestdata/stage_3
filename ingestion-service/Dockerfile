# Dockerfile - uses cached base image that already has Java
# Uses the existing cached service image as base to avoid network downloads
FROM stage_3-ingestion-service:latest AS base

# Start fresh but copy the JRE from the base
FROM scratch AS jre-source

# Actually, let's use the cached image directly and just replace the JAR
FROM stage_3-ingestion-service:latest

# Copy our new pre-built JAR over the old one
COPY ingestion-service/target/ingestion-service-*.jar /app/ingestion-service/target/ingestion-service-1.0.0.jar

EXPOSE 7001 5701

# Keep the original CMD from cached image
CMD ["java", "-jar", "target/ingestion-service-1.0.0.jar"]
