# Simple Dockerfile - uses pre-built JAR
# IMPORTANT: Run 'mvn clean package -DskipTests' locally before docker-compose!
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy pre-built JAR
COPY ingestion-service/target/ingestion-service-*.jar /app/app.jar

EXPOSE 7001 5701

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
