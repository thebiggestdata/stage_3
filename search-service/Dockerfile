FROM eclipse-temurin:17-jdk

COPY target/search-service-1.0.0.jar /app/search.jar

EXPOSE 7003 5701

WORKDIR /app

ENTRYPOINT ["java", "-jar", "search.jar"]