FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

COPY trajectory-service/trajectory-file-service/target/trajectory-file-service-1.0.0.jar app.jar

ENV SPRING_PROFILES_ACTIVE="prod"
EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
