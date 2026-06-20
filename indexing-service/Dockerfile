FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY target/indexing-service-1.0-SNAPSHOT.jar /app/indexing.jar

EXPOSE 5701 5702 5703 7002

ENTRYPOINT ["java", "-jar", "indexing.jar"]
