FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

COPY pom.xml .
COPY trajectory-common/pom.xml trajectory-common/
COPY trajectory-common/trajectory-common-core/pom.xml trajectory-common/trajectory-common-core/
COPY trajectory-common/trajectory-common-web/pom.xml trajectory-common/trajectory-common-web/
COPY trajectory-common/trajectory-common-mysql/pom.xml trajectory-common/trajectory-common-mysql/
COPY trajectory-common/trajectory-common-cache/pom.xml trajectory-common/trajectory-common-cache/
COPY trajectory-common/trajectory-common-auth/pom.xml trajectory-common/trajectory-common-auth/
COPY trajectory-common/trajectory-common-log/pom.xml trajectory-common/trajectory-common-log/
COPY trajectory-common/trajectory-common-rabbitmq/pom.xml trajectory-common/trajectory-common-rabbitmq/
COPY trajectory-api/pom.xml trajectory-api/
COPY trajectory-api/trajectory-api-user/pom.xml trajectory-api/trajectory-api-user/
COPY trajectory-api/trajectory-api-log/pom.xml trajectory-api/trajectory-api-log/
COPY trajectory-api/trajectory-api-ai/pom.xml trajectory-api/trajectory-api-ai/
COPY trajectory-service/pom.xml trajectory-service/
COPY trajectory-service/trajectory-ai-service/pom.xml trajectory-service/trajectory-ai-service/

RUN apk add --no-cache maven && \
    mvn dependency:go-offline -pl trajectory-service/trajectory-ai-service -am -B

COPY trajectory-common trajectory-common
COPY trajectory-api trajectory-api
COPY trajectory-service trajectory-service

RUN mvn clean package -pl trajectory-service/trajectory-ai-service -am -DskipTests -B

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata

COPY --from=builder /build/trajectory-service/trajectory-ai-service/target/trajectory-ai-service-1.0.0.jar app.jar

ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENV SPRING_PROFILES_ACTIVE="prod"

EXPOSE 8089

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8089/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE -jar app.jar"]
