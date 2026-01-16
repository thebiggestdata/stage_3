# Simple Dockerfile - uses pre-built JAR
# IMPORTANT: Run 'mvn clean package -DskipTests' locally before docker-compose!
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy pre-built JAR
COPY search-service/target/search-service-*.jar /app/app.jar

EXPOSE 7003 5703

ENTRYPOINT ["java", "-jar", "/app/app.jar"]


