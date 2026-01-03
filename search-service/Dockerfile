FROM maven:3.9-eclipse-temurin-17

WORKDIR /app

COPY pom.xml /app/pom.xml

RUN mvn -N install

COPY search-service/pom.xml /app/search-service/pom.xml

COPY search-service/src /app/search-service/src

WORKDIR /app/search-service

RUN mvn -q -e -DskipTests package

CMD ["java", "-jar", "target/search-service-1.0-SNAPSHOT.jar"]


