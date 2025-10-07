# syntax=docker/dockerfile:1
FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV APP_PORT=8080
ENV CSV_ROOT=/data
ENV EXPORT_DIR=/app/exports
ENV PDF_TITLE="Stocks Dashboard"
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
VOLUME ["/data", "/app/exports"]
ENTRYPOINT ["java","-jar","/app/app.jar"]

