FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

COPY pom.xml .
COPY src ./src
RUN mvn -q -e -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/indexer-service-1.0-SNAPSHOT.jar app.jar

EXPOSE 7002
ENTRYPOINT ["java", "-jar", "app.jar"]
