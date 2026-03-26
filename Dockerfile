# Run Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Su dung file jar da build local tai target/
COPY target/*.jar app.jar
COPY .env .env

# Cai Python3 + edge-tts de tao audio thật
RUN apk add --no-cache python3 py3-pip \
    && pip3 install edge-tts --break-system-packages

# Expose port
EXPOSE 8080

# Environment variables will be overridden by docker-compose
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "app.jar"]
