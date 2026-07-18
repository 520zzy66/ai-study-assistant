# ============================================
# AI Study Assistant - Backend Dockerfile
# ============================================
# Build: docker compose build backend
# Run:   docker compose up -d
# ============================================

FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre

LABEL maintainer="AI Study Assistant"

# 创建非 root 用户
RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

# 复制 JAR 包
COPY --from=builder /build/target/*.jar app.jar

# 创建上传目录并设置权限
RUN mkdir -p /app/uploads && chown -R appuser:appuser /app

# 切换到非 root 用户
USER appuser

# 暴露端口
EXPOSE 3001

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]
