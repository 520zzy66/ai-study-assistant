# AI Study Assistant - Task List

## v1.0 已完成（见下方折叠）

<details>
<summary>点击展开 v1.0 完成项</summary>

### Phase 1 基础环境
- [x] 初始化 Spring Boot 项目
- [x] 初始化 Vue3 项目
- [x] 配置 MySQL 和 MyBatis-Plus
- [x] 配置 Spring AI 和 DeepSeek API

### Phase 2 用户模块
- [x] 用户表设计
- [x] 登录接口
- [x] 注册接口
- [x] JWT 鉴权

### Phase 3 文档管理
- [x] 文件上传接口（MaterialController.upload）
- [x] PDF/Word/TXT 解析（TikaDocumentParser）
- [x] 文本切片（ChunkSplitter，800字/片，100字重叠）
- [x] 文件列表/详情/删除接口
- [x] 异步处理流程（解析→切片→入库）

### Phase 4 AI 核心功能
- [x] AI 知识总结（AiSummaryService + POST /ai/summary/{id}）
- [x] AI 文档问答 RAG（AiQaService + RagService + POST /ai/qa + POST /ai/qa/stream）
- [x] AI 自动生成练习题（AiQuizService + POST /ai/quiz/{id} + POST /ai/quiz/{batchId}/answer）
- [x] AI 学习计划生成（AiPlanService + POST /ai/plan + GET /ai/plan）
- [x] 错题本管理（GET /ai/quiz/wrong + PUT /ai/quiz/wrong/{id}/master）
- [x] AiController 统一 AI 接口（10 个端点）

### Phase 5 前端页面
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

### Phase 5.5 在线资源搜索
- [x] 后端 OnlineResourceService + OnlineResourceController（Jsoup + DuckDuckGo）
- [x] 前端 resource.js API 封装
- [x] OnlineSearch.vue 页面（搜索 + 导入 + 进度轮询）
- [x] 路由配置 /resource/search
- [x] 侧边栏导航入口

### Phase 5.6 异步任务模式扩展
- [x] AiSummary.vue 改为异步任务模式（generateSummaryAsync + taskStore.watchTask）
- [x] AiPlan.vue 改为异步任务模式（generatePlanAsync + taskStore.watchTask）
- [x] 两个页面均支持 onMounted 恢复未完成任务

### Phase 6 收尾
- [x] 历史记录接口（HistoryController + HistoryService）
- [x] 代码审查问题修复（3 轮共 20+ 项）
- [x] 联调测试（启动前后端，验证完整流程）
- [x] Profile/Settings 功能完善（修改密码 API + 偏好持久化）
- [x] Docker 部署（docker-compose + Nginx 反向代理）
- [x] 完善 README

</details>

---

# v2.0 迭代计划

## Phase 7 RAG 检索升级 🔴 [核心]

> 目标：从关键词匹配升级为混合检索，显著提升文档问答准确率

### 7.1 中文分词集成
- [ ] 集成 jieba-analysis 到 RagService
- [ ] 实现中文分词器 JiebaTokenizer
- [ ] 分词结果缓存（避免重复分词）
- [ ] 停用词过滤（的、了、是、在 等）

### 7.2 BM25 关键词检索
- [ ] 实现 BM25 算法（基于 TF-IDF 改进）
- [ ] 计算文档平均长度、IDF 值
- [ ] 支持查询扩展（同义词）
- [ ] BM25 检索器单元测试

### 7.3 向量检索集成
- [ ] 实现 EmbeddingProvider 接口（调用 bge-small-zh 或在线 API）
- [ ] 文档切片向量化（入库时生成 embedding）
- [ ] 向量相似度检索（余弦相似度）
- [ ] 向量索引优化（HNSW 或 IVF）

### 7.4 混合检索与重排序
- [ ] 实现 HybridSearchService（向量 + BM25）
- [ ] 混合分数计算（加权融合）
- [ ] （可选）Reranking 重排序模型集成
- [ ] Top-K 结果截取与上下文拼接

### 7.5 检索质量评估
- [ ] 构建测试数据集（问答对）
- [ ] 检索准确率/召回率指标
- [ ] A/B 测试框架（新旧算法对比）

---

## Phase 8 会话管理与持久化 🔴

> 目标：支持多会话管理，对话历史持久化

### 8.1 后端会话接口
- [ ] 设计会话表 `chat_session`（id, user_id, title, created_at, updated_at）
- [ ] 设计消息表 `chat_message`（id, session_id, role, content, created_at）
- [ ] Mapper 层：ChatSessionMapper, ChatMessageMapper
- [ ] Service 层：ChatSessionService（CRUD + 分页）
- [ ] Controller 层：ChatSessionController
  - POST /chat/session — 创建会话
  - GET /chat/session/list — 会话列表
  - PUT /chat/session/{id} — 更新标题
  - DELETE /chat/session/{id} — 删除会话
  - GET /chat/session/{id}/messages — 获取消息历史

### 8.2 前端会话管理
- [ ] 对接后端会话 API
- [ ] 会话列表按时间分组（今天、过去7天、更早）
- [ ] 会话切换时加载对应消息
- [ ] 新建会话清空当前对话
- [ ] 会话重命名（双击编辑）
- [ ] 会话删除（确认弹窗）

### 8.3 对话上下文优化
- [ ] 会话消息自动保存（流式完成后）
- [ ] 上下文窗口管理（最近 N 条消息）
- [ ] 会话搜索功能（标题/内容模糊搜索）
- [ ] 会话固定/置顶功能

---

## Phase 9 安全加固与限流 🔴

> 目标：防止资源滥用，提升系统安全性

### 9.1 接口限流
- [ ] 集成 Bucket4j 限流库
- [ ] 实现限流注解 @RateLimit
- [ ] AI 接口限流配置（每用户每分钟 10 次）
- [ ] 文件上传限流（每用户每天 50 次）
- [ ] 限流响应返回 429 + Retry-After 头

### 9.2 输入校验增强
- [ ] 文件内容安全检查（恶意代码检测）
- [ ] 问题长度限制（最大 2000 字符）
- [ ] SQL 注入/XSS 防护检查
- [ ] 敏感词过滤（可选）

### 9.3 认证优化
- [ ] JWT 层错误码与业务层错误码分离
- [ ] Token 黑名单机制（登出后失效）
- [ ] 并发登录限制（可选）
- [ ] 添加 AccessDeniedException 处理

### 9.4 日志与审计
- [ ] 操作日志记录（登录、上传、AI 调用）
- [ ] 敏感操作告警
- [ ] 日志脱敏处理

---

## Phase 10 单元测试覆盖 🔴

> 目标：核心模块测试覆盖率 ≥ 60%

### 10.1 后端测试
- [ ] 测试基础设施搭建（JUnit 5 + Mockito）
- [ ] AiQuizService 测试
  - parseQuizResponse — AI 返回解析
  - submitAnswer — 答案提交与判分
- [ ] RagService 测试
  - search — 检索算法
  - BM25 评分计算
- [ ] ChunkSplitter 测试
  - split — 文本切片
  - 边界情况处理
- [ ] JsonUtils 测试
  - cleanJsonResponse — JSON 清洗
- [ ] JwtUtil 测试
  - generateToken / validateToken

### 10.2 前端测试
- [ ] 测试基础设施搭建（Vitest + Vue Test Utils）
- [ ] useMarkdown 测试
  - renderMarkdown — Markdown 渲染
  - XSS 防护验证
- [ ] Pinia Store 测试
  - useAiStore — 状态管理
  - useUserStore — 用户状态
- [ ] API 模块测试
  - 请求拦截器
  - 响应处理

### 10.3 测试配置
- [ ] 测试数据库配置（H2 或 Testcontainers）
- [ ] CI 测试流水线配置
- [ ] 测试覆盖率报告生成

---

## Phase 11 错误处理优化 🟡

> 目标：类型安全，错误处理更清晰

### 11.1 VO 类定义
- [ ] 定义 AiSummaryVO（总结响应）
- [ ] 定义 AiQaResponseVO（问答响应）
- [ ] 定义 AiQuizResponseVO（出题响应）
- [ ] 定义 AiPlanResponseVO（学习计划响应）
- [ ] 定义 WrongQuestionListVO（错题列表响应）

### 11.2 Controller 层优化
- [ ] 替换所有 Map<String, Object> 返回值
- [ ] 统一 Result<T> 泛型使用
- [ ] 添加 @ApiResponse 注解（Swagger 文档）

### 11.3 异常处理增强
- [ ] 定义更多业务异常类型
- [ ] 异常信息国际化（中英文）
- [ ] 异常堆栈脱敏（生产环境）

---

## Phase 12 前端重构 🟡

> 目标：提升代码可维护性和用户体验

### 12.1 AiChat.vue 组件拆分
- [ ] ChatSidebar.vue — 会话管理侧边栏
  - [ ] ConversationItem.vue — 会话列表项
  - [ ] NewChatButton.vue — 新建对话按钮
- [ ] MessageList.vue — 消息列表
  - [ ] UserMessage.vue — 用户消息气泡
  - [ ] AssistantMessage.vue — AI 消息气泡
  - [ ] TypingIndicator.vue — 打字指示器
  - [ ] MessageActions.vue — 消息操作按钮
- [ ] ChatInput.vue — 输入区域
  - [ ] MaterialSelector.vue — 资料选择器
  - [ ] FileUpload.vue — 文件上传
- [ ] WelcomeScreen.vue — 欢迎屏幕

### 12.2 公共组件优化
- [ ] BaseMarkdown.vue — Markdown 渲染组件
- [ ] BaseAvatar.vue — 头像组件
- [ ] BaseTooltip.vue — 提示组件
- [ ] 组件文档生成（Storybook 或 Vuepress）

### 12.3 状态管理优化
- [ ] useAiStore 持久化（sessionStorage）
- [ ] useChatStore 新增（会话管理）
- [ ] Token 刷新逻辑统一到 useUserStore

---

## Phase 13 暗色模式与主题 🟢

> 目标：完整的主题切换支持

### 13.1 CSS 变量体系
- [ ] 定义亮色主题变量
- [ ] 定义暗色主题变量
- [ ] 主题切换机制（class 切换）
- [ ] 系统主题检测（prefers-color-scheme）

### 13.2 组件主题适配
- [ ] 侧边栏暗色适配
- [ ] 消息气泡暗色适配
- [ ] 输入框暗色适配
- [ ] 卡片和弹窗暗色适配

### 13.3 主题持久化
- [ ] 用户主题偏好存储（localStorage）
- [ ] 主题切换动画（平滑过渡）
- [ ] 主题切换组件（设置页面）

---

## Phase 14 响应式与移动端 🟢

> 目标：移动端可用性提升

### 14.1 移动端布局
- [ ] 侧边栏抽屉式交互
- [ ] 消息气泡宽度自适应
- [ ] 输入框全宽显示
- [ ] 触摸手势支持（滑动返回）

### 14.2 移动端优化
- [ ] 虚拟键盘适配
- [ ] 长按菜单（复制、转发）
- [ ] 下拉刷新
- [ ] 无限滚动加载

### 14.3 PWA 支持（可选）
- [ ] Service Worker 配置
- [ ] 离线缓存策略
- [ ] 添加到主屏幕

---

## Phase 15 学习数据分析 🟡

> 目标：用户学习行为可视化

### 15.1 数据统计接口
- [ ] 学习时长统计（按日/周/月）
- [ ] 知识点掌握度分析
- [ ] 错题分布统计
- [ ] AI 使用频率统计
- [ ] 统计数据缓存（Redis）

### 15.2 可视化图表
- [ ] 学习时长折线图（ECharts）
- [ ] 知识点掌握雷达图
- [ ] 错题分布饼图
- [ ] 学习热力图（GitHub 风格）

### 15.3 Dashboard 升级
- [ ] 统计卡片优化（带动画）
- [ ] 趋势分析（同比/环比）
- [ ] 学习建议（基于数据）
- [ ] 导出报表（PDF/图片）

---

## Phase 16 导出与分享 🟢

> 目标：内容可迁移，支持协作学习

### 16.1 导出功能
- [ ] 对话记录导出（Markdown/TXT）
- [ ] AI 总结导出（Markdown/PDF）
- [ ] 学习计划导出（PDF）
- [ ] 错题本导出（PDF/Excel）

### 16.2 分享功能
- [ ] 生成分享链接
- [ ] 分享页面渲染
- [ ] 分享权限控制（仅查看/可编辑）
- [ ] 分享统计（查看次数）

---

## Phase 17 工程化与部署 🟢

> 目标：完善工程化和部署流程

### 17.1 CI/CD 流水线
- [ ] GitHub Actions 配置
- [ ] 自动化测试（后端 JUnit + 前端 Vitest）
- [ ] Docker 镜像构建与推送
- [ ] 自动化部署脚本

### 17.2 监控与日志
- [ ] 日志规范（log.info → log.debug 降级）
- [ ] 结构化日志（JSON 格式）
- [ ] 健康检查接口（/actuator/health）
- [ ] 错误告警机制（邮件/Webhook）

### 17.3 文档完善
- [ ] API 文档（Swagger UI 可访问性确认）
- [ ] 部署文档（Docker / 手动部署）
- [ ] 开发者指南（贡献规范）
- [ ] 用户使用手册

---

## Phase 18 性能优化 🟢

> 目标：提升系统响应速度和并发能力

### 18.1 后端优化
- [ ] 数据库查询优化（N+1 问题）
- [ ] 热点数据缓存（Redis）
- [ ] 异步任务优化（线程池配置）
- [ ] 连接池配置（HikariCP）

### 18.2 前端优化
- [ ] 路由懒加载优化
- [ ] 图片懒加载
- [ ] 虚拟滚动（长列表）
- [ ] 代码分割策略

### 18.3 AI 调用优化
- [ ] 流式响应缓冲优化
- [ ] 请求合并（批量 Embedding）
- [ ] 结果缓存（相同问题）
- [ ] 超时重试机制

---

## 优先级说明

| 优先级 | 标记 | 说明 |
|--------|------|------|
| 高 | 🔴 | 核心功能/稳定性，必须优先完成 |
| 中 | 🟡 | 重要改进，建议第二阶段完成 |
| 低 | 🟢 | 体验优化，可按需选择 |

## 建议执行顺序

```
第一阶段（2-3 周）：Phase 7 + Phase 8 + Phase 9
  → RAG 升级 + 会话持久化 + 安全加固

第二阶段（2 周）：Phase 10 + Phase 11
  → 单元测试 + 错误处理优化

第三阶段（2 周）：Phase 12 + Phase 13
  → 前端重构 + 暗色模式

第四阶段（按需）：Phase 14 - Phase 18
  → 功能扩展与性能优化
```

## 里程碑

| 里程碑 | 目标 | 预计时间 |
|--------|------|----------|
| M1 | RAG 检索升级 + 会话持久化 | 第 3 周末 |
| M2 | 安全加固 + 测试覆盖 | 第 5 周末 |
| M3 | 前端重构 + 主题系统 | 第 7 周末 |
| M4 | 数据分析 + 导出功能 | 第 9 周末 |
| M5 | 工程化 + 性能优化 | 第 11 周末 |
