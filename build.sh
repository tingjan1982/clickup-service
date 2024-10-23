#!/bin/zsh

./gradlew bootJar

docker build --no-cache --build-arg JAR_FILE=build/libs/clickup-service-0.0.1-SNAPSHOT.jar -t joelin/clickup-service:latest ./
