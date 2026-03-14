FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

COPY trajectory-service/trajectory-log-service/target/trajectory-log-service-1.0.0.jar app.jar

ENV SPRING_PROFILES_ACTIVE="prod"
EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
