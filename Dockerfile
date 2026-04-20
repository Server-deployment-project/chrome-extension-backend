# syntax=docker/dockerfile:1.7

# ===== Build stage =====
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# 复制 pom.xml 先下载依赖（利用Docker层缓存）
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B -q

# 复制源代码并编译
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -B -q

# ===== Runtime stage =====
FROM eclipse-temurin:17-jre-alpine

# 安装基础工具和健康检查
RUN apk add --no-cache curl tini

# 创建应用用户（提高安全性，避免以root运行）
RUN addgroup -g 1000 appuser && \
    adduser -D -u 1000 -G appuser appuser

WORKDIR /app

# 从builder阶段复制编译好的jar
COPY --from=builder /build/target/*.jar app.jar

# 创建日志目录并设置权限
RUN mkdir -p /var/log && \
    chown -R appuser:appuser /app /var/log

# 切换到非root用户
USER appuser

# 暴露端口
EXPOSE 8001

# 健康检查（每30秒检查一次，失败3次后标记为不健康）
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=40s \
    CMD curl -f http://localhost:8001/api/v1/health || exit 1

# 使用tini作为PID 1进程，确保优雅的信号传递
ENTRYPOINT ["/sbin/tini", "--"]

# 启动命令
CMD ["java", "-jar", \
     "-Xms512m", "-Xmx1024m", \
     "-Dspring.profiles.active=docker", \
     "app.jar"]
