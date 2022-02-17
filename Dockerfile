FROM openjdk:17-alpine
COPY target/*.jar app.jar
USER 1001
ENTRYPOINT ["java", "-server", "-jar", "app.jar"]
