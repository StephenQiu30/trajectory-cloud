FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

COPY . .
RUN mvn clean package -pl trajectory-service/trajectory-notification-service -am -DskipTests -B

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata

COPY --from=builder /build/trajectory-service/trajectory-notification-service/target/trajectory-notification-service-1.0.0.jar app.jar

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENV SPRING_PROFILES_ACTIVE="prod"

EXPOSE 8083

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8083/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE -jar app.jar"]
