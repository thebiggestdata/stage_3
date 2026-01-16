# Simple Dockerfile - uses pre-built JAR
# IMPORTANT: Run 'mvn clean package -DskipTests' locally before docker-compose!
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy pre-built JAR
COPY indexing-service/target/indexing-service-*.jar /app/app.jar

EXPOSE 7002 5702

ENTRYPOINT ["java", "-jar", "/app/app.jar"]