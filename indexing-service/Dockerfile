FROM maven:3.9-eclipse-temurin-17

WORKDIR /app

COPY pom.xml /app/pom.xml

RUN mvn -N install

COPY indexing-service/pom.xml /app/indexing-service/pom.xml

COPY indexing-service/src /app/indexing-service/src

WORKDIR /app/indexing-service

RUN mvn -q -e -DskipTests package

CMD ["java", "-jar", "target/indexing-service-1.0-SNAPSHOT.jar"]