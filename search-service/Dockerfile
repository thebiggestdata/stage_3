FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copia el jar compilado
COPY target/search-service-1.0-SNAPSHOT.jar app.jar

# Expone los puertos distribuidos y el de la API
EXPOSE 5703 5702 5701 7003

# Al ser un JAR empaquetado con Shade, esto es lo único que necesitas para arrancar
ENTRYPOINT ["java", "-cp", "app.jar", "com.thebiggestdata.Main"]