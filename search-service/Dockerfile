# Dockerfile - uses cached base image that already has Java
FROM stage_3-search-service:latest

# Copy our new pre-built JAR over the old one
COPY search-service/target/search-service-*.jar /app/search-service/target/search-service-1.0.0.jar

EXPOSE 7003 5703

CMD ["java", "-jar", "target/search-service-1.0.0.jar"]


