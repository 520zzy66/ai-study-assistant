# ============================================
# AI Study Assistant - Backend Dockerfile
# ============================================
# Build: mvnw package -DskipTests
# Run:   docker build -t ai-study-backend .
# ============================================

FROM eclipse-temurin:21-jre

LABEL maintainer="AI Study Assistant"

# 创建非 root 用户
RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

# 复制 JAR 包
COPY target/ai-study-assistant-0.0.1-SNAPSHOT.jar app.jar

# 创建上传目录并设置权限
RUN mkdir -p /app/uploads && chown -R appuser:appuser /app

# 切换到非 root 用户
USER appuser

# 暴露端口
EXPOSE 3001

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]
