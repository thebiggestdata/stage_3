FROM maven:3.9-eclipse-temurin-17

WORKDIR /app

COPY pom.xml /app/pom.xml

RUN mvn -N install

COPY ingestion-service/pom.xml /app/ingestion-service/pom.xml

COPY ingestion-service/src /app/ingestion-service/src

WORKDIR /app/ingestion-service

RUN mvn -q -e -DskipTests package

EXPOSE 5702 5701 7001

CMD ["java", "-jar", "target/ingestion-service-1.0.0.jar"]
