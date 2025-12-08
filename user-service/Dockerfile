FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn install -N -DskipTests
COPY user-service/pom.xml user-service/
COPY user-service user-service/
WORKDIR /app/user-service
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/user-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

