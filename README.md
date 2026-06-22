# 🎓 AI 智能学习助手

一个基于 Spring Boot 3 + Spring AI + Vue3 的全栈 AI 智能学习助手系统。

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://www.java.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-green.svg)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.4-brightgreen.svg)](https://vuejs.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## ✨ 功能特性

### 📚 文档管理
- 支持 PDF、Word、TXT 等多种格式文档上传
- 智能文档解析与文本切片
- 文档列表管理（查看、删除）

### 🤖 AI 核心功能
- **知识总结**：AI 自动生成文档核心知识点总结
- **智能问答**：基于 RAG 的文档问答，支持多轮对话
- **自动出题**：根据文档内容自动生成练习题（单选、判断、简答）
- **学习计划**：根据学习目标和时间生成个性化学习计划

### 📝 学习辅助
- **错题本**：自动收集错题，支持标记掌握状态
- **学习历史**：记录所有 AI 交互历史
- **个人中心**：用户信息管理与偏好设置

### 🎨 现代化 UI
- 响应式设计，支持多端适配
- Emerald 绿主题配色
- 流畅的交互体验

---

## 🛠️ 技术栈

### 后端
| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 21 | LTS 版本 |
| Spring Boot | 3.3.0 | 核心框架 |
| Spring AI | 1.0.0-M6 | AI 能力集成 |
| MyBatis-Plus | 3.5.7 | ORM 框架 |
| MySQL | 8.0 | 数据库 |
| JWT | 0.12.6 | 身份认证 |
| Apache Tika | 2.9.1 | 文档解析 |

### 前端
| 技术 | 版本 | 说明 |
|------|------|------|
| Vue.js | 3.4+ | 前端框架 |
| Vite | 5.4+ | 构建工具 |
| Element Plus | 2.7+ | UI 组件库 |
| Pinia | 2.1+ | 状态管理 |
| Axios | 1.7+ | HTTP 客户端 |
| Markdown-it | 14.1+ | Markdown 渲染 |

### AI 能力
- **大语言模型**：DeepSeek API（兼容 OpenAI 接口）
- **Embedding 模型**：bge-small-zh（本地部署）
- **向量检索**：Vector Search + BM25 混合检索
- **Reranking**：可选的重排序模块

---

## 🚀 快速开始

### 环境要求

- **JDK**: 21+
- **Node.js**: 18+
- **Maven**: 3.8+
- **MySQL**: 8.0+

### 1. 克隆项目

```bash
git clone https://github.com/your-username/ai-study-assistant.git
cd ai-study-assistant
```

### 2. 配置环境变量

项目已包含 `.env` 配置文件，修改其中的配置即可：

```env
# 数据库配置
DB_USERNAME=root
DB_PASSWORD=root

# DeepSeek API 配置（必填）
DEEPSEEK_API_KEY=your_api_key

# JWT 密钥（已配置默认值，生产环境请修改）
JWT_SECRET=d2hhdGV2ZXJ5bG9uZ3NlY3JldGtleWZvcmp3dHRvZ2VuZXJhdGVkMTIzNA==
```

> ⚠️ **重要**：`DEEPSEEK_API_KEY` 必须配置，否则 AI 功能无法使用。

### 3. 初始化数据库

```bash
# 登录 MySQL 后执行
mysql -u root -p < sql/init.sql
```

### 4. 启动后端

**方式 1：使用启动脚本（推荐）**

```bash
# Linux/Mac
chmod +x start-backend.sh
./start-backend.sh

# Windows
start-backend.bat
```

**方式 2：手动启动**

```bash
cd backend

# Linux/Mac - 需要先设置环境变量
export JWT_SECRET="d2hhdGV2ZXJ5bG9uZ3NlY3JldGtleWZvcmp3dHRvZ2VuZXJhdGVkMTIzNA=="
export DEEPSEEK_API_KEY="your_api_key"
./mvnw spring-boot:run

# Windows
set JWT_SECRET=d2hhdGV2ZXJ5bG9uZ3NlY3JldGtleWZvcmp3dHRvZ2VuZXJhdGVkMTIzNA==
set DEEPSEEK_API_KEY=your_api_key
./mvnw.cmd spring-boot:run
```

后端服务将在 `http://localhost:3001/api` 启动。

### 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端服务将在 `http://localhost:3000` 启动。

### 6. 访问应用

打开浏览器访问 `http://localhost:3000`，注册账号后即可使用。

- **Swagger API 文档**: `http://localhost:3001/api/swagger-ui.html`

---

## 🐳 Docker 部署

### 使用 Docker Compose（推荐）

> ⚠️ **前置要求**：需要先安装 [Docker Desktop](https://www.docker.com/products/docker-desktop/)

```bash
# 配置环境变量
cp .env.example .env
# 编辑 .env 填写配置

# 一键启动（Docker Desktop 新版本使用 docker compose）
docker compose up -d

# 如果是旧版本 Docker，使用：
# docker-compose up -d
```

服务将启动在：
- 前端：`http://localhost:3000`
- 后端 API：`http://localhost:3001/api`
- MySQL：`localhost:3306`

### 停止服务

```bash
docker compose down

# 旧版本：
# docker-compose down
```

---

## 📁 项目结构

```
ai-study-assistant/
├── .claude/                    # Claude 开发配置
│   ├── CLAUDE.md              # 项目规范
│   ├── task-list.md           # 任务清单
│   └── prompts/               # Prompt 模板
├── backend/                    # 后端项目
│   ├── src/main/java/
│   │   └── com.study/
│   │       ├── controller/    # 控制器层
│   │       ├── service/       # 业务逻辑层
│   │       ├── mapper/        # 数据访问层
│   │       ├── entity/        # 实体类
│   │       ├── dto/           # 数据传输对象
│   │       ├── vo/            # 视图对象
│   │       ├── ai/            # AI 相关模块
│   │       ├── config/        # 配置类
│   │       ├── security/      # 安全认证
│   │       └── common/        # 公共组件
│   └── src/main/resources/
│       └── application.yml    # 配置文件
├── frontend/                   # 前端项目
│   ├── src/
│   │   ├── api/               # API 请求
│   │   ├── views/             # 页面组件
│   │   ├── components/        # 通用组件
│   │   ├── stores/            # 状态管理
│   │   ├── router/            # 路由配置
│   │   └── styles/            # 样式文件
│   └── public/                # 静态资源
├── sql/                        # 数据库脚本
│   └── init.sql               # 初始化脚本
├── docs/                       # 项目文档
│   ├── spec.md                # 开发规格文档
│   └── ui-design-spec.md      # UI 设计规范
├── docker-compose.yml          # Docker 编排
└── README.md                   # 项目说明
```

---

## 📡 API 接口

### 认证接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/user/register` | 用户注册 |
| POST | `/api/user/login` | 用户登录 |
| GET | `/api/user/info` | 获取用户信息 |
| PUT | `/api/user/password` | 修改密码 |

### 文档管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/material/upload` | 上传文档 |
| GET | `/api/material/list` | 文档列表 |
| GET | `/api/material/{id}` | 文档详情 |
| DELETE | `/api/material/{id}` | 删除文档 |

### AI 功能

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/ai/summary/{id}` | 文档总结 |
| POST | `/api/ai/qa` | 文档问答 |
| POST | `/api/ai/qa/stream` | 流式问答 |
| POST | `/api/ai/quiz/{id}` | 生成练习题 |
| POST | `/api/ai/quiz/answer` | 提交答案 |
| GET | `/api/ai/quiz/wrong` | 错题本列表 |
| PUT | `/api/ai/quiz/wrong/{id}/master` | 标记掌握 |
| POST | `/api/ai/plan` | 生成学习计划 |
| GET | `/api/ai/plan` | 获取学习计划 |

### 历史记录

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/history/qa` | 问答历史 |
| GET | `/api/history/quiz` | 练习历史 |
| GET | `/api/history/summary` | 总结历史 |

> 📖 完整 API 文档请访问：`http://localhost:3001/api/swagger-ui.html`

---

## 🔧 配置说明

### 应用配置 (application.yml)

```yaml
spring:
  ai:
    openai:
      api-key: ${DEEPSEEK_API_KEY}
      base-url: ${DEEPSEEK_BASE_URL}
      chat:
        options:
          model: deepseek-chat
  
  datasource:
    url: jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DATABASE}
    username: ${MYSQL_USER}
    password: ${MYSQL_PASSWORD}

jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000  # 24 小时
```

### AI 模型配置

系统默认使用 DeepSeek Chat 模型，可在配置文件中修改：

```yaml
spring:
  ai:
    openai:
      chat:
        options:
          model: deepseek-chat  # 或 deepseek-coder 等
          temperature: 0.7
          max-tokens: 2000
```

---

## 🤝 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

---

## 📝 开发规范

请参阅 [.claude/CLAUDE.md](.claude/CLAUDE.md) 了解完整的开发规范。

主要原则：
- 遵循 Spec 驱动开发模式
- 代码审查必须通过 6 维检查
- 保持向后兼容
- 一次只完成一个任务

---

## 📄 License

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

---

## 🙏 致谢

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring AI](https://spring.io/projects/spring-ai)
- [Vue.js](https://vuejs.org/)
- [Element Plus](https://element-plus.org/)
- [DeepSeek](https://www.deepseek.com/)

---

## 📞 联系方式

如有问题或建议，请通过以下方式联系：

- 提交 Issue
- 发送邮件至：your-email@example.com

---

**⭐ 如果这个项目对你有帮助，请给个 Star 支持一下！**
