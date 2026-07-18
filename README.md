# AI Study Assistant

面向中国软件杯 A3 赛题的智能学习助手系统。项目以“个性化资源生成 + 多智能体协作 + 多模态学习体验”为核心，围绕高校课程学习场景，提供资料管理、AI 问答、知识总结、自动出题、学习计划和资源工坊等能力。

## 项目亮点

- **多智能体资源工坊**：围绕单份学习资料，协同生成讲解文档、思维导图、个性化题库、学习路径、多模态脚本、知识配图和播客音频。
- **个性化学习闭环**：结合学习目标、难度、截止日期、每日学习时长和资料内容，生成适合当前学习阶段的资源组合。
- **多模态资源生成**：接入科大讯飞 TTS 与 HiDream，支持知识点播客化、资源包封面和知识图解生成。
- **资料关联 AI 问答**：支持关联学习资料或资料文件夹进行对话，辅助学生快速理解资料内容。
- **资源包 PDF 导出**：资源工坊结果可导出图文并茂的 PDF，便于提交、分享和课堂展示。
- **云端模型部署友好**：Docker 部署默认使用云端模型能力，不依赖本地 Ollama，适合团队快速演示。
- **内容安全与防幻觉约束**：Prompt 和安全 Agent 对资料依据、来源说明、内部结构泄露等问题做了约束。

## 技术栈

后端：

- Java 21
- Spring Boot 3.5.2
- Spring AI 1.1.2
- Spring AI Alibaba 1.1.2.0
- MyBatis-Plus 3.5.7
- PostgreSQL 16 + pgvector
- Redis
- JWT

前端：

- Vue 3
- Vite
- Element Plus
- Pinia
- Axios
- Markdown-it

AI 与多模态：

- 科大讯飞星火 Lite：主对话模型
- 科大讯飞 TTS：学习资源播客音频生成
- 科大讯飞 HiDream：知识配图与资源包封面生成
- 阿里云百炼 `text-embedding-v4`：Embedding
- 阿里云百炼 `qwen-vl-plus`：上传图片理解

## 核心功能

### 资料管理

- 支持 PDF、Word、TXT、Markdown、图片等学习资料上传。
- 支持资料文件夹管理。
- 支持资料解析、预览和后续 AI 任务关联。

### AI 问答

- 支持流式回答。
- 支持关联单份资料、资料文件夹或临时上传资料。
- 支持历史对话记录。
- Prompt 约束不会直接暴露底层知识库目录和文件结构。

### AI 总结

- 根据资料内容生成 Markdown 总结。
- 提取核心知识点、重点概念和复习建议。

### 自动出题

- 根据学习资料生成练习题。
- 支持答案解析和错题记录。

### 学习计划

- 根据学习目标、考试日期和每日学习时间生成计划。
- 支持学习任务拆解和阶段安排。

### 资源工坊

- 多 Agent 流水线生成个性化资源包。
- 支持生成讲解文档、思维导图、题库、学习路径、多模态脚本。
- 支持生成播客音频和知识配图。
- 支持导出 Markdown 和 PDF。
- 支持 Agent 执行链路和生成进度展示。

## 快速启动

推荐使用 Docker Compose 启动项目。

### 1. 安装 Docker Desktop

请先安装并启动 Docker Desktop。

Docker 官网：

```text
https://www.docker.com/products/docker-desktop/
```

### 2. 进入项目根目录

Windows PowerShell 示例：

```powershell
cd C:\path\to\ai-study-assistant
```

### 3. 创建 `.env`

复制环境变量模板：

```powershell
Copy-Item .env.example .env
```

打开 `.env`：

```powershell
notepad .env
```

填写模型、数据库和 JWT 配置。

### 4. 必要环境变量

`.env` 至少需要包含以下配置：

```env
DB_PASSWORD=your_database_password
JWT_SECRET=your_base64_jwt_secret

AI_PROVIDER=xfyun-spark
AI_BASE_URL=https://spark-api-open.xf-yun.com/v1
AI_COMPLETIONS_PATH=/chat/completions
AI_API_KEY=your_xfyun_spark_api_password
AI_CHAT_MODEL=lite

XFYUN_APP_ID=your_xfyun_app_id
XFYUN_API_KEY=your_xfyun_api_key
XFYUN_API_SECRET=your_xfyun_api_secret
XFYUN_IMAGE_PROVIDER=hidream

DASHSCOPE_API_KEY=your_dashscope_api_key
DASHSCOPE_EMBEDDING_MODEL=text-embedding-v4
DASHSCOPE_EMBEDDING_DIMENSION=1024
DASHSCOPE_VL_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
DASHSCOPE_VL_MODEL=qwen-vl-plus

FRONTEND_PORT=3000
BACKEND_PORT=3001
DB_PORT=5433
REDIS_PORT=3002
```

生成 `JWT_SECRET` 的 PowerShell 命令：

```powershell
$bytes = New-Object byte[] 64
[Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

### 5. 一键启动

```powershell
docker-compose up -d --build
```

新版 Docker 也可以使用：

```powershell
docker compose up -d --build
```

### 6. 查看服务状态

```powershell
docker-compose ps
```

正常情况下会启动 4 个服务：

- `ai-study-postgres`
- `ai-study-redis`
- `ai-study-backend`
- `ai-study-frontend`

### 7. 访问系统

前端页面：

```text
http://localhost:3000
```

后端健康检查：

```text
http://localhost:3001/api/actuator/health
```

返回以下内容表示后端启动成功：

```json
{"status":"UP"}
```

### 8. 查看日志

查看后端日志：

```powershell
docker-compose logs -f backend
```

查看前端日志：

```powershell
docker-compose logs -f frontend
```

### 9. 停止服务

```powershell
docker-compose down
```

## 页面入口

启动后访问 `http://localhost:3000`。

主要功能入口：

- 首页：学习概览与资源推荐
- 学习资料：资料上传、管理与预览
- AI 问答：资料关联问答
- AI 总结：资料知识总结
- 自动出题：题目生成与练习
- 学习计划：个性化计划生成
- 资源工坊：多 Agent 个性化资源包生成
- 历史记录：AI 任务和对话历史
- 用户中心：个人信息与学习画像

## Docker 服务说明

| 服务 | 说明 | 默认端口 |
|------|------|----------|
| frontend | Vue 前端应用 | 3000 |
| backend | Spring Boot 后端服务 | 3001 |
| postgres | PostgreSQL + pgvector | 5433 |
| redis | Redis 缓存 | 3002 |

Docker 启动时会自动执行数据库初始化脚本：

```text
sql/schema-pg.sql
sql/data-pg.sql
```

系统知识库默认挂载目录：

```text
backend/src/main/resources/knowledge-bank
```

后端容器内路径：

```text
/app/resources/knowledge-bank
```

## 本地开发

如需本地开发而不是 Docker 演示，需要安装：

- JDK 21
- Node.js 18+
- PostgreSQL 16 + pgvector
- Redis

启动后端：

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

启动前端：

```powershell
cd frontend
npm install
npm run dev
```

## 项目结构

```text
ai-study-assistant/
├─ backend/                         Spring Boot 后端
│  ├─ src/main/java/com/study/
│  └─ src/main/resources/
│     ├─ application.yml
│     ├─ application-dev.yml
│     ├─ application-prod.yml
│     ├─ agents/
│     ├─ prompts/
│     └─ knowledge-bank/
├─ frontend/                        Vue 前端
│  ├─ src/api/
│  ├─ src/components/
│  ├─ src/stores/
│  └─ src/views/
├─ sql/                             数据库脚本
├─ docs/                            项目文档
├─ docker-compose.yml               Docker Compose 配置
├─ .env.example                     环境变量模板
└─ README.md
```

## 参赛价值

本项目围绕高等教育中“资源繁杂、个体差异大、学习路径不清晰、反馈不及时”的问题，构建了一个以多智能体协作为核心的个性化学习资源生成系统。

系统不仅能完成传统 AI 总结和问答，还能将资料转化为可复习、可练习、可展示的资源包，并通过音频和配图提升学习材料的可理解性与传播性，符合 A3 赛题对个性化、多模态、多智能体系统的要求。
