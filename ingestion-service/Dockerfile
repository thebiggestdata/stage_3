FROM maven:3.9-eclipse-temurin-17

WORKDIR /app

COPY pom.xml /app/pom.xml

RUN mvn -N install

COPY target/ingestion-service-1.0.0.jar /app/ingestion.jar

EXPOSE 5702 5701 7001

WORKDIR /app/ingestion-service

RUN mvn -q -e -DskipTests package

ENTRYPOINT ["java", "-jar", "ingestion.jar"]