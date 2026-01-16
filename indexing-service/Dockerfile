# Dockerfile - uses cached base image that already has Java
FROM stage_3-indexing-service:latest

# Copy our new pre-built JAR over the old one
COPY indexing-service/target/indexing-service-*.jar /app/indexing-service/target/indexing-service-1.0.0.jar

EXPOSE 7002 5702

CMD ["java", "-jar", "target/indexing-service-1.0.0.jar"]