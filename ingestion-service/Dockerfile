FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY target/ingestion-service-1.0-SNAPSHOT.jar /app/ingestion.jar

EXPOSE 5701 5702 5703 7001

ENTRYPOINT ["java", "-jar", "ingestion.jar"]
