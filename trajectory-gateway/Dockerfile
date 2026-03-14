FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

COPY pom.xml .
COPY trajectory-common/pom.xml trajectory-common/
COPY trajectory-common/trajectory-common-core/pom.xml trajectory-common/trajectory-common-core/
COPY trajectory-common/trajectory-common-web/pom.xml trajectory-common/trajectory-common-web/
COPY trajectory-common/trajectory-common-auth/pom.xml trajectory-common/trajectory-common-auth/
COPY trajectory-common/trajectory-common-log/pom.xml trajectory-common/trajectory-common-log/
COPY trajectory-api/pom.xml trajectory-api/
COPY trajectory-api/trajectory-api-user/pom.xml trajectory-api/trajectory-api-user/
COPY trajectory-api/trajectory-api-log/pom.xml trajectory-api/trajectory-api-log/

COPY trajectory-gateway/pom.xml trajectory-gateway/

RUN mvn dependency:go-offline -pl trajectory-gateway -am -B

COPY trajectory-common trajectory-common
COPY trajectory-api trajectory-api
COPY trajectory-gateway trajectory-gateway

RUN mvn clean package -pl trajectory-gateway -am -DskipTests -B

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata

COPY --from=builder /build/trajectory-gateway/target/trajectory-gateway-1.0.0.jar app.jar

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENV SPRING_PROFILES_ACTIVE="prod"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE -jar app.jar"]
