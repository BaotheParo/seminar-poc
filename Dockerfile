# Build Stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true

# Run Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
## NOTE:
## Do not COPY `.env` into the image because `.env` is typically not committed (secrets)
## and fresh clones would fail to build. Use docker-compose `environment`/`env_file` instead.

# Cai Python3 + edge-tts de tao audio thật
RUN apk add --no-cache python3 py3-pip \
    && pip3 install edge-tts --break-system-packages

# Expose port
EXPOSE 8080

# Environment variables will be overridden by docker-compose
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "app.jar"]
