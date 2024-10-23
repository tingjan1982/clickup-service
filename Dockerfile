FROM amazoncorretto:23-alpine-jdk
LABEL authors="Joe Lin"
EXPOSE 8080

ARG JAR_FILE
COPY ${JAR_FILE} /app.jar

ENTRYPOINT ["sh", "-c", "java -jar /app.jar"]