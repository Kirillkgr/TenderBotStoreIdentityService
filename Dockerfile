#### 🐳 Dockerfile (базовый)

FROM openjdk:17-jdk-slim
COPY target/identity-service.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
