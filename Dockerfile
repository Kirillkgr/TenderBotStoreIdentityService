# syntax=docker/dockerfile:1.6
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -e dependency:go-offline
COPY src/ /app/src/
# For production image build, skip tests inside container to avoid relying on test-only resources
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -e package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Application listens on 9900 by default (see src/main/resources/application.yml)
EXPOSE 9900
ENTRYPOINT ["java", "-jar", "app.jar"]
