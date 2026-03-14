FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

COPY trajectory-gateway/target/trajectory-gateway-1.0.0.jar app.jar

ENV SPRING_PROFILES_ACTIVE="prod"
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
