# Build Stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY .env .env

# Cai Python3 + edge-tts de tao audio thật
RUN apk add --no-cache python3 py3-pip \
    && pip3 install edge-tts --break-system-packages

# Expose port
EXPOSE 8080

# Environment variables will be overridden by docker-compose
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "app.jar"]
