# AI Study Assistant - Task List

## Phase 1 基础环境
- [x] 初始化 Spring Boot 项目
- [x] 初始化 Vue3 项目
- [x] 配置 MySQL 和 MyBatis-Plus
- [x] 配置 Spring AI 和 DeepSeek API

## Phase 2 用户模块
- [x] 用户表设计
- [x] 登录接口
- [x] 注册接口
- [x] JWT 鉴权

## Phase 3 文档管理
- [x] 文件上传接口（MaterialController.upload）
- [x] PDF/Word/TXT 解析（TikaDocumentParser）
- [x] 文本切片（ChunkSplitter，800字/片，100字重叠）
- [x] 文件列表/详情/删除接口
- [x] 异步处理流程（解析→切片→入库）

## Phase 4 AI 核心功能
- [x] AI 知识总结（AiSummaryService + POST /ai/summary/{id}）
- [x] AI 文档问答 RAG（AiQaService + RagService + POST /ai/qa + POST /ai/qa/stream）
- [x] AI 自动生成练习题（AiQuizService + POST /ai/quiz/{id} + POST /ai/quiz/{batchId}/answer）
- [x] AI 学习计划生成（AiPlanService + POST /ai/plan + GET /ai/plan）
- [x] 错题本管理（GET /ai/quiz/wrong + PUT /ai/quiz/wrong/{id}/master）
- [x] AiController 统一 AI 接口（10 个端点）

## Phase 5 前端页面
- [x] 登录页（Login.vue — 左右分屏，去紫色渐变）
- [x] 首页 Dashboard（Dashboard.vue — 色块统计卡片）
- [x] 文档管理页面（Material.vue — 表格 + 拖拽上传）
- [x] AI 对话页面（AiChat.vue — 顶部工具栏 + Emerald 气泡）
- [x] AI 总结页面（AiSummary.vue — 简洁布局）
- [x] AI 出题页面（AiQuiz.vue — 左侧色条题目卡片）
- [x] AI 学习计划页面（AiPlan.vue — 自定义时间线）
- [x] 错题本页面（WrongQuestion.vue — 红绿答案区分）
- [x] 历史记录页面（History.vue — 表格 + 详情弹窗）
- [x] UI 设计系统（theme.css + global.css — Emerald 绿 + Zinc 中性色）

## Phase 6 收尾
- [x] 历史记录接口（HistoryController + HistoryService）
- [x] 代码审查问题修复（3 轮共 20+ 项）
- [x] 联调测试（启动前后端，验证完整流程）
- [x] Profile/Settings 功能完善（修改密码 API + 偏好持久化）
- [x] Docker 部署（docker-compose + Nginx 反向代理）
- [x] 完善 README
