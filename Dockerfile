# Docker tips: https://www.docker.com/blog/9-tips-for-containerizing-your-spring-boot-code/
FROM amazoncorretto:25-alpine-jdk
LABEL authors="Joe Lin"
EXPOSE 8080

#WORKDIR /app
#ARG JAR_FILE
COPY build/libs/clickup-service-0.0.1-SNAPSHOT.jar /app.jar

ENTRYPOINT ["sh", "-c", "java -jar /app.jar"]