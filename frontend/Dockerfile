# ============================================
# AI Study Assistant - Frontend Dockerfile
# ============================================
# Multi-stage build: Node build + Nginx serve
# Build: docker build -t ai-study-frontend .
# ============================================

# ---- Stage 1: Build ----
FROM node:18-alpine AS builder

WORKDIR /app

# 复制依赖文件
COPY package.json package-lock.json ./

# 安装依赖
RUN npm ci

# 复制源码
COPY . .

# 构建生产版本
RUN npm run build

# ---- Stage 2: Serve ----
FROM nginx:alpine

LABEL maintainer="AI Study Assistant"

# 创建非 root 用户运行 nginx
RUN addgroup -S appuser && adduser -S appuser -G appuser

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 设置权限
RUN chown -R appuser:appuser /usr/share/nginx/html && \
    chown -R appuser:appuser /var/cache/nginx && \
    chown -R appuser:appuser /var/log/nginx && \
    touch /var/run/nginx.pid && chown appuser:appuser /var/run/nginx.pid

# 切换到非 root 用户
USER appuser

# 暴露端口
EXPOSE 3000

CMD ["nginx", "-g", "daemon off;"]
