FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY target/benchmarking-1.0-SNAPSHOT.jar app.jar

EXPOSE 5704

ENTRYPOINT ["java", "-jar", "app.jar"]