# 第一阶段：构建模块
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# 仅复制必要的构建定义和源码
COPY pom.xml .
COPY trajectory-common trajectory-common
COPY trajectory-api trajectory-api
COPY trajectory-service trajectory-service

# 使用 BuildKit 缓存构建特定模块
RUN --mount=type=cache,target=/root/.m2 mvn clean package -pl trajectory-service/trajectory-mail-service -am -DskipTests -B

# 第二阶段：运行环境
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 设置系统时区
RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata

# 复制构件，重命名为 app.jar
COPY --from=builder /build/trajectory-service/trajectory-mail-service/target/trajectory-mail-service-1.0.0.jar app.jar

# 针对 2GB 内存服务器的 JVM 优化
ENV JAVA_OPTS="-Xms128m -Xmx144m -XX:+UseSerialGC -XX:MaxGCPauseMillis=200"
ENV SPRING_PROFILES_ACTIVE="prod"

# 暴露端口
EXPOSE 8085

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8085/actuator/health || exit 1

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE -jar app.jar"]
