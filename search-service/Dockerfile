FROM eclipse-temurin:17-jdk

WORKDIR /app
COPY target/search-service-1.0-SNAPSHOT.jar app.jar

EXPOSE 5701 5702 5703 7003

ENTRYPOINT ["java", "-cp", "app.jar", "com.thebiggestdata.Main"]
