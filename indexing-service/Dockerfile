FROM eclipse-temurin:17-jdk

COPY target/indexing-service-1.0.0.jar /app/indexing.jar

EXPOSE 7002 5701

WORKDIR /app

ENTRYPOINT ["java", "-jar", "indexing.jar"]