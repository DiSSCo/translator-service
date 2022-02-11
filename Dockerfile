FROM openjdk:17-slim
COPY target/*.jar app.jar
USER 1001
ENTRYPOINT ["java", "-server", "-jar", "app.jar"]
