# AI Study Assistant - Task List

## v1.0 已完成（见下方折叠）

<details>
<summary>点击展开 v1.0 完成项</summary>

### Phase 1 基础环境
- [x] 初始化 Spring Boot 项目
- [x] 初始化 Vue3 项目
- [x] 配置 PostgreSQL 16 + pgvector 和 MyBatis-Plus
- [x] 配置 Spring AI 和 Xiaomi MiMo API（DeepSeek 作为备选）

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

## v1.5 已完成但未在 v1.0 记录的功能

> 以下功能在 v1.0 之后、v2.0 规划之前实现，但未及时更新到任务列表。

### 基础设施增强
- [x] **BoundedChatMemory**（`ai/memory/BoundedChatMemory.java`）— 有界对话记忆，LRU 淘汰策略，最大 1000 会话，防止内存溢出
- [x] **ChatRequest Builder**（`ai/client/ChatRequest.java`）— 统一 AI 请求参数 Builder，支持 systemPrompt / temperature / memory / stream 等组合
- [x] **MaterialValidator**（`ai/validator/MaterialValidator.java`）— 资料归属校验逻辑抽取，统一校验 materialId 属于当前用户且状态为 ready
- [x] **MaterialContentReader**（`ai/MaterialContentReader.java`）— 资料内容读取工具，RAG 检索无结果时的降级读取（全量切片 → 原始文件）
- [x] **UserConverter**（`common/UserConverter.java`）— User 实体与 UserVO 转换器

### 安全加固
- [x] **SecurityUtils**（`common/SecurityUtils.java`，135行）— URL 安全校验（防 SSRF：协议白名单 + 内网 IP 过滤 + DNS 重绑定防护，IPv4/IPv6/IPv6-mapped-IPv4）+ 文件名清理（防路径穿越）
- [x] **LoginAttemptServiceImpl**（`service/impl/LoginAttemptServiceImpl.java`，68行）— Redis 登录失败锁定（5 次/15 分钟），Redis 不可用自动降级，TTL 防永久锁定
- [x] **UserContextFilter**（`config/UserContextFilter.java`）— 请求完成后自动清理 ThreadLocal 用户上下文，防止内存泄漏

### 异步处理增强
- [x] **MaterialAsyncProcessor 失败重试**（`service/impl/MaterialAsyncProcessor.java`）— 定时扫描失败任务（30 分钟间隔），最多重试 3 次，限 24 小时内的失败记录，支持手动重试接口
- [x] **原子状态更新** — 并发保护，防止同一文档被重复处理

### VO 层完善
- [x] 8 个 VO 类：UserVO、MaterialVO、MaterialDetailVO、AiSummaryVO、AiQaResponseVO、AiQuizResponseVO、AiPlanResponseVO、WrongQuestionListVO
- [x] 14 个请求 DTO：参数校验注解完善（@NotBlank / @Size / @Pattern 等）

### 前端增强
- [x] **异步任务全局管理**（`stores/task.js`）— 2 秒轮询任务进度，支持 watchTask / stopWatching / resumePending，网络错误重试（最多 5 次）
- [x] **Token 自动刷新**（`api/index.js`）— 响应拦截器检查 `x-new-token` 头，自动刷新本地 Token
- [x] **QuestionBank.vue** — 题库管理页（批次卡片列表、搜索、重命名/删除、收藏、重新作答即时判分）
- [x] **Profile.vue** — 用户中心（头像/昵称修改）
- [x] **Settings.vue** — 设置页（修改密码 + 通知偏好持久化 + 关于信息）

---

# v2.0 迭代计划

## Phase UI-1: 核心工作台视觉优化（2026-07-16）✅

- [x] 备份优化前前端源码（`backups/frontend-20260716-194812.zip`）
- [x] 新增 `docs/ui-design-spec.md`，明确品牌、排版、布局和交互规范
- [x] 补齐全局字体、行高、图标、层级和辅助色 Design Token
- [x] AI 问答页优化：对话历史分组、资料关联、文档式回答、专注输入区
- [x] 学习资料页优化：文件管理工作台、统一文件夹面板、精简表格行操作
- [x] 保持现有 API、请求参数、流式问答和资料管理业务行为不变
- [x] 前端生产构建验证通过

---

## Phase UI-2: 全站界面一致性优化（2026-07-16）✅

- [x] 备份第一阶段完成态（`backups/frontend-ui1-20260716-211241.zip`）
- [x] 全局导航按“学习 / AI 工具 / 复习”重组，保持原路由不变
- [x] 统一阅读页、工作台页、数据页的内容宽度、间距和页头层级
- [x] 首页、AI 总结、导图工作台、学习计划视觉优化
- [x] AI 出题、题库、错题本、历史记录视觉与键盘交互优化
- [x] 用户中心、设置、登录页视觉优化
- [x] 修复导图工作台页头操作插槽名不匹配、错题页文案错误
- [x] 补齐全站缺失 Design Token，扫描结果为 0 个缺失令牌
- [x] 保持 API、路由、请求参数和业务流程不变
- [x] 前端生产构建验证通过（Vite，2059 modules）
- [x] 按 6 维 Checklist 完成本次前端变更代码审查

---

## Phase UI-4: 首页卡片有效性收尾（2026-07-18）✅

- [x] 首页统计卡片改为基于现有资料、历史、学习计划和进度接口的真实数据口径
- [x] “学习进度 / 距离目标日期 / 今日任务”接入最新学习计划与计划进度，不再停留在占位逻辑
- [x] 今日任务支持直接打卡，复用 `PUT /ai/plan/{planId}/day/{dayIndex}`，不改变后端接口契约
- [x] 继续学习卡片根据资料状态进入问答或总结工作流，最近动态卡片可跳转历史记录
- [x] 新增学习计划列表前端 API 封装，保持现有 `/ai/plan` 后端契约不变
- [x] 修复学习计划页生成/恢复后未设置当前计划 ID 导致进度刷新不可用的问题
- [x] 调整倒计时组件为内容型组件，避免首页出现卡片套卡片
- [x] 同步更新 `docs/ui-design-spec.md` 的首页卡片有效性规范
- [x] 前端生产构建验证通过（Vite，2087 modules）
- [x] 按 6 维 Checklist 完成本次首页卡片变更代码审查

---

## Phase A3-1: 中国软件杯 A3 资源工坊（2026-07-16）🚧

> 目标：围绕“基于大模型的个性化资源生成与学习多智能体系统开发”，在不推翻现有工作流的前提下，新增资源包生成编排层。

- [x] 明确 MVP 范围：资源工坊、单资料生成、默认 14 天学习路径、多模态脚本包先不调用真实讯飞接口
- [x] 新增资源包异步任务入口：`POST /ai/task/resource-package`
- [x] 新增 `ResourcePackageService`，复用总结、思维导图、出题、学习计划能力
- [x] 新增多模态脚本 Prompt，生成 PPT 大纲、图像提示词、语音讲解稿、微课分镜和实操案例
- [x] 新增前端资源工坊页面 `/ai/resource-package`
- [x] 侧边栏与首页快捷入口接入资源工坊
- [x] 资源工坊支持最近资源包历史查看与结果恢复
- [x] 首页新增学习画像、最近资料和资源包历史驱动的个性化资源推荐入口
- [x] 资源工坊结果支持资源类型概览、Markdown 导出和 JSON 完整结果导出
- [x] 资源包结果补齐真实阶段进度、资源清单、Agent 分工、质量安全说明和演示摘要复制
- [x] 新增 `docs/agent-upgrade-spec.md`，明确后续全功能 Agent 化渐进式改造方案
- [x] 复审并升级 Agent Spec：冻结 Phase A 多 Agent、Tool、进度、取消、部分失败、旧数据兼容、Evaluation 口径、讯飞 Provider 与前端状态契约
- [x] Phase A 验收补强：统一资源开关默认语义、补全全失败判定、取消态 cancelled、Trace ID、Agent/Tool JavaDoc 与 Orchestrator 回归测试
- [x] 新增 `docs/xfyun-multimodal-spec.md`，规划讯飞 TTS 播客化、文生图知识配图、资源资产管理和后续星火/教育模型迁移方案
- [ ] 后续接入讯飞智能 PPT、图片生成、超拟人语音合成和文本合规 Provider
- [ ] 后续扩展文件夹级资源包生成

---

## Phase A3-2: 讯飞多模态 spec 实施（2026-07-17）✅

> 目标：严格按照 `docs/xfyun-multimodal-spec.md` 实现讯飞 TTS 播客音频 + 文生图知识配图 + 资源资产管理 + 前端面板接入。改动严格局限于该 spec 范围。

### 配置层
- [x] `XfyunProperties`：新增 tts（endpoint/voice/timeoutMs/maxTextBytes/enabled）和 image（endpoint/model/defaultSize/maxImageCount/enabled）子配置；提供 `isTtsConfigured()`/`isImageConfigured()` 契约方法
- [x] `ResourceAssetProperties`：storageDir / maxAudioBytes / maxImageBytes
- [x] `application.yml`：补齐讯飞 TTS/TTI 配置块占位

### 数据层
- [x] `ResourceAsset` 实体 + `ResourceAssetMapper`
- [x] `sql/migrate-resource-asset-pg.sql`（新建迁移文件）
- [x] `sql/schema-pg.sql`（追加 resource_asset 表定义）

### Provider 抽象与讯飞实现
- [x] `SpeechSynthesisProvider` / `ImageGenerationProvider` 接口
- [x] `XfyunSignatureUtil` + 单元测试（HMAC-SHA256 鉴权 URL 生成）
- [x] `XfyunTtsProvider`（JDK `java.net.http.WebSocket`，MP3，多帧独立解码后拼接字节）
- [x] `XfyunImageGenerationProvider`（JDK `HttpClient`，base64 PNG/JPEG 解码，错误码 10043/10040/"审核"关键字映射 CONTENT_REJECTED）
- [x] `ProviderErrorType` / `AssetGenerationResult` / `AssetGenerationRequest` / `AudioSynthesisRequest` / `ImageGenerationRequest` / `ImagePromptPlan` 模型

### 服务层
- [x] `ResourceAssetStorageService`：UUID 文件名、路径穿越防护（`..`+startsWith）、文件头校验（ID3/frame sync/PNG/JPEG/WebP）、扩展名白名单（png/jpg/jpeg/webp）
- [x] `ResourceAssetConverter`：Entity↔VO，previewUrl/downloadUrl 拼接（不暴露 storagePath）
- [x] `ResourceAssetService`：pending→generating→success/failed 状态流转，metadata 保留 originalText/originalPrompt 支持重试，权限过滤（userId 强校验），capabilities 视图

### Agent 与编排
- [x] `PromptTemplates`：播客脚本生成模板、图片提示词生成模板（含安全约束）
- [x] `ResourceGenerationTools`：`@Tool` 暴露给 LLM 的 generateMultimodalScript / generatePodcastScript / generateImagePrompts
- [x] `MultimodalAgent` 升级为脚本+资产生成 Agent，进度编排 84/87/90/93/94，Provider 失败不拖垮资源包
- [x] `ResourceOrchestratorAgent`：编排 Profile → Summary → MindMap → Quiz → Plan → Multimodal → Safety
- [x] `ResourcePackageService` + `AiTaskService`：异步任务、进度轮询（98 保存结果 / 100 完成）
- [x] `SafetyTools` / `SafetyAgent`：质量安全校验

### Controller 层
- [x] `ResourceAssetController`：4 个端点
  - `GET /api/resource-assets/capabilities` — 能力查询
  - `GET /api/resource-assets/package/{packageId}` — 列表
  - `GET /api/resource-assets/{assetId}/download` — 下载（权限校验 + 文件不存在自动 markFileMissing）
  - `POST /api/resource-assets/{assetId}/retry` — 重试失败资产

### 前端
- [x] `api/ai.js`：新增 capabilities / listPackageAssets / downloadAsset / retryAsset API 封装
- [x] `AssetStatusBadge.vue`：资产状态徽章（pending/generating/success/failed/cancelled）
- [x] `PodcastAssetCard.vue`：播客音频卡片（内置 audio 播放器 + 下载 + 重试）
- [x] `ImageAssetGallery.vue`：图片画廊（封面 + 解析图，下载 + 重试）
- [x] `ResourceAssetPanel.vue`：资产面板（能力开关 + 音频区 + 图片区 + 空状态）
- [x] `ResourceWorkshop.vue`：资源工坊页面接入 ResourceAssetPanel
- [x] 前端生产构建验证通过（Vite，无错误）

### 测试（spec §13.1 要求的 7 个测试类）
- [x] `XfyunSignatureUtilTest`（8 个用例）
- [x] `XfyunTtsProviderTest`（12 个用例，含多帧累积、错误码映射、配置缺失）
- [x] `XfyunImageGenerationProviderTest`（17 个用例，含 PNG/JPEG 头检测、审核失败映射、HTTP 401/429/500）
- [x] `ResourceAssetStorageServiceTest`（29 个用例，含路径穿越、文件头、扩展名白名单）
- [x] `ResourceAssetServiceTest`（25 个用例，含状态流转、权限过滤、重试、能力视图）
- [x] `MultimodalAgentAssetTest`（18 个用例，含开关控制、失败隔离、进度里程碑 84/87/90/93/94）
- [x] `ResourceAssetControllerTest`（19 个用例，含权限校验、下载、重试）
- [x] `ResourceOrchestratorAgentTest`（3 个用例，编排回归）
- [x] **测试结果：131 tests run, 0 failures, 0 errors**

### 实施期间修复的主代码缺陷
- [x] `MultimodalAgent`：补齐进度 84 上报（spec §4.2 要求"正在整理多模态脚本"）
- [x] `XfyunTtsProvider`：将"拼接 base64 字符串再整体解码"改为"每帧独立解码后拼接字节"，匹配讯飞真实响应（每帧 audio 是独立带 padding 的 base64）
- [x] `ResourceAssetStorageService.sanitizeExtension`：扩展名白名单校验（spec §11.3 图片只允许 PNG/JPEG/WebP），非法扩展名回退到 Content-Type

### 未实施项（按 spec 要求或用户决定）
- [ ] §12 ChatProviderRouter：本阶段不实现（用户决定）
- [ ] §4.2 SafetyAgent 不再单独上报进度（用户决定：MultimodalAgent 占 84-94）

---


## Phase 1-4: AI 功能升级（2026-07-07）

> 基于 `docs/ai-upgrade-spec.md` 的四阶段升级计划

- [x] Phase 1: 流式总结 + Agent 编排 + 思维导图 ✅
  - [x] 新增 agent-summary.yml 配置
  - [x] AiSummaryService 新增 generateSummaryStream + generateMindMap 方法
  - [x] AiController 新增 /summary/stream + /summary/mindmap 端点
  - [x] 前端 ai.js 新增流式 API + MindMapTree.vue 组件
  - [x] AiSummary.vue 流式效果 + 思维导图展示

- [x] Phase 2: 出题新题型 + 数学题判分优化 ✅
  - [x] 扩展 PromptTemplates 出题模板（新题型 + 难度描述）
  - [x] 扩展 GenerateQuizRequest DTO（新题型数量字段）
  - [x] AiQuizService 新增判分逻辑（fill_blank/multi_choice/math_fill）
  - [x] AiQuiz.vue 新增题型 UI

- [x] Phase 3: 错题本增强（重做错题 + 统计图表）✅
  - [x] 后端：AiQuizService 新增 getWrongQuestionsForRepractice + getWrongQuestionStats 方法
  - [x] 后端：AiController 新增 GET /quiz/wrong/repractice + GET /quiz/wrong/stats 端点
  - [x] 前端：quiz.js 新增 getRepracticeQuestions + getWrongQuestionStats API
  - [x] 前端：WrongQuestion.vue 统计面板（题型/来源分布 + 趋势图）+ 重做错题模式

- [x] Phase 4: 错题 PDF 导出 ✅
  - [x] pom.xml 添加 OpenPDF 依赖（librepdf 2.0.2）
  - [x] WrongQuestionPdfService — PDF 生成（表格布局，题型/题目/答案/来源）
  - [x] AiController 新增 GET /quiz/wrong/export 端点（返回 PDF 文件）
  - [x] quiz.js 新增 exportWrongQuestionsPdf API（blob 下载）
  - [x] WrongQuestion.vue 工具栏添加"导出 PDF"按钮
  - [x] PDF 导出重设计：题库批次支持逐套导出，错题本支持勾选题目导出，统一试卷风格（题目与答案解析分离）

---

## Phase 6.5 StateGraph 工作流架构 ✅

> 目标：将 AI 对话功能封装为 StateGraph 状态图工作流，替代命令式编排器
>
> **当前状态**：已完成。Graph 架构（GeneralNode → LocalAnswerNode/ExpertAgentNode → END）已实现并编译通过。

### 6.5.1 Graph 核心
- [x] **WorkflowGraphService**（`workflow/graph/WorkflowGraphService.java`）— StateGraph 构建与执行，替代旧的 AiWorkflowOrchestrator
- [x] **RouteKeys**（`workflow/graph/RouteKeys.java`）— State key 常量定义（新增 MEMORY_CONTEXT、RECENT_CONTEXT）
- [x] **WorkflowChatResult**（`workflow/graph/WorkflowChatResult.java`）— 工作流结果 DTO（从 orchestrator 包移入 graph 包）

### 6.5.2 节点实现
- [x] **GeneralNode**（`workflow/node/GeneralNode.java`）— 重写：会话向量化 + 上下文注入（画像/记忆/历史/近期对话）+ 三级路由 + 简单问题回答
- [x] **LocalAnswerNode**（`workflow/node/LocalAnswerNode.java`）— Level 0/1 本地回答节点
- [x] **ExpertAgentNode**（`workflow/node/ExpertAgentNode.java`）— 专家 Agent 节点，内部路由到三个 ExpertAgentService
- [x] **MultimodalNode**（`workflow/node/MultimodalNode.java`）— 资料预处理节点（保留）

### 6.5.3 专家 Agent 服务
- [x] **ExpertAgentService** 接口 — 改为 `answer(OverAllState state)`
- [x] **AbstractExpertAgentService** — 简化 buildPrompt，从 OverAllState 读取上下文
- [x] **CivilExpertAgentService** — 考公专家（适配新接口）
- [x] **GraduateExpertAgentService** — 考研专家（适配新接口）
- [x] **GeneralQaAgentService** — 通用专家（适配新接口）

### 6.5.4 集成与清理
- [x] **AiController** — 改用 WorkflowGraphService
- [x] **AgentExecutionLogService** — import 改为 graph 包
- [x] **删除** AiWorkflowOrchestrator.java（旧编排器）
- [x] **删除** orchestrator/WorkflowChatResult.java（移入 graph 包）

### 6.5.5 文档
- [x] **workflow-orchestrator-spec.md** — v2.0 Graph 架构设计文档
- [x] **workflow-implementation-guide.md** — 详细实现说明（面向小白）
- [x] **spec.md** — 新增 Spec-10: StateGraph 工作流

---

## Phase 7 知识库搭建与 RAG 检索升级 🔴 [核心]

> 目标：搭建向量知识库，从关键词匹配升级为语义检索 + BM25 混合检索，显著提升文档问答准确率
>
> **当前状态**：PgVector、Ollama bge-m3（1024 维）、BM25 和 HybridSearchService 已实现；正式资料上传已写入 VectorStore，StateGraph 专家通过 KnowledgeTools 调用混合检索。

### 7.1 向量存储基础设施
- [x] 选型：PgVector（PostgreSQL 16 扩展，与业务数据共存）
- [x] Docker 部署 PostgreSQL + pgvector
- [x] 设计 Spring AI `vector_store` 表（embedding vector(1024)）
- [x] 引入 Spring AI PgVector VectorStore 依赖
- [x] 编写 VectorStore 初始化 SQL

### 7.2 Embedding 模型集成
- [x] 实现 EmbeddingProvider 接口（OllamaEmbeddingProvider）
  - 当前方案：本地 Ollama bge-m3（1024 维）
- [x] Embedding 维度单元测试（1024 维契约）
- [x] 批量 Embedding 方法实现（embedBatch）
- [x] 激活 `application.yml` 中的 `ai.embedding.*` 配置项

### 7.3 知识库构建流程
- [x] 切片入库时自动生成 embedding（修改 MaterialAsyncProcessor.doProcess）
- [x] 存储向量到向量数据库（chunk ↔ vector 关联，稳定 Document ID 支持幂等重试）
- [x] 存储元数据（material_id, user_id, folder_id, chunk_id, chunk_index, file_type）
- [x] 资料删除时同步清理向量数据
- [ ] 重新索引接口（资料更新后重建向量）

### 7.4 中文分词与 BM25 检索
- [x] 集成 jieba-analysis（Bm25Service）
- [x] 实现 jieba 中文分词
- [x] 停用词过滤（的、了、是、在 等）
- [x] 实现 BM25 算法
- [x] 计算文档平均长度、IDF 值
- [ ] BM25 检索器单元测试

### 7.5 混合检索与重排序
- [x] 实现 HybridSearchService（向量语义检索 + BM25 关键词检索）
- [x] 使用 RRF 融合向量与 BM25 排名
- [x] Top-K 结果截取
- [x] 激活 `application.yml` 中的 `ai.rag.*` 配置项
- [x] 将 HybridSearchService 接入 StateGraph 专家 Tool 主问答链路（按可信 userId/materialId 限定）
- [ ] （可选）Reranking 重排序模型集成（交叉编码器精排）

### 7.5.1 临时会话资料 ✅
- [x] 临时上传接口返回服务端 `uploadToken`，按用户/会话隔离并设置 7 天 TTL
- [x] 重构 MultimodalNode：仅处理临时资料，不按正式资料 `materialId` 读取
- [x] 独立预处理服务完成文档/图片提取、切片和临时索引，MultimodalNode 首问生成缓存摘要
- [x] 临时 RAG Tool 仅检索当前 `userId + conversationId + uploadToken`
- [x] “添加至我的资料”幂等接口；成功后清理临时文件、切片和向量
- [x] AI Chat 附件改为临时上传，处理完成前禁止发送
- [x] 学习资料增加“临时资料”Tab，支持问答、转正式和删除
- [x] 支持 PDF/DOC/DOCX/TXT/MD 与 PNG/JPG/JPEG/WEBP（Qwen2.5-VL）

### 7.6 检索质量评估
- [ ] 构建测试数据集（10+ 问答对，覆盖精确匹配 + 语义等价场景）
- [ ] 检索准确率/召回率指标
- [ ] A/B 测试（关键词匹配 vs 向量检索 vs 混合检索）
- [ ] 检索结果可视化（调试用）

---

## Phase 8 会话管理与持久化 🟡

> 目标：支持多会话管理，对话历史持久化
>
> **当前状态**：前端会话管理已完整实现（AiChat.vue ~2050行），后端使用 `ai_chat_history` 表 + `conversationId` 字段
> 存储对话，未按 spec 设计独立的 `chat_session` / `chat_message` 表。功能可用，后端 schema 可按需升级。

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

### 8.2 前端会话管理 ✅
- [x] 对接后端会话 API（AiChat.vue 通过 ai.js store + askQuestionStream 实现）
- [x] 会话列表按时间分组（今天、过去7天、更早）
- [x] 会话切换时加载对应消息
- [x] 新建会话清空当前对话
- [x] 会话重命名（双击编辑）
- [x] 会话删除（确认弹窗）

### 8.3 对话上下文优化 ✅
- [x] 会话消息自动保存（流式完成后 doOnComplete 保存到 ai_chat_history）
- [x] 上下文窗口管理（BoundedChatMemory，LRU 淘汰，最大 1000 会话，窗口 10 条）
- [ ] 会话搜索功能（标题/内容模糊搜索）— 未实现
- [x] 会话固定/置顶功能（AiChat.vue 支持 pin）

---

## Phase 9 安全加固与限流 🟡

> 目标：防止资源滥用，提升系统安全性
>
> **当前状态**：输入校验和认证优化部分已完成（SecurityUtils + LoginAttemptServiceImpl），限流和日志审计待做。

### 9.1 接口限流
- [ ] 集成 Bucket4j 限流库
- [ ] 实现限流注解 @RateLimit
- [ ] AI 接口限流配置（每用户每分钟 10 次）
- [ ] 文件上传限流（每用户每天 50 次）
- [ ] 限流响应返回 429 + Retry-After 头

### 9.2 输入校验增强 ✅ 部分完成
- [ ] 文件内容安全检查（恶意代码检测）— 未实现
- [ ] 问题长度限制（最大 2000 字符）— 未实现
- [x] SQL 注入/XSS 防护检查（SecurityUtils.java：SSRF 防护，协议白名单 + 内网 IP 过滤 + DNS 重绑定防护，支持 IPv4/IPv6/IPv6-mapped-IPv4）
- [x] 路径穿越防护（SecurityUtils.java：文件名清理，替换危险字符并限制长度）
- [ ] 敏感词过滤（可选）— 未实现

### 9.3 认证优化 ✅ 部分完成
- [ ] JWT 层错误码与业务层错误码分离 — 未实现
- [ ] Token 黑名单机制（登出后失效）— 未实现
- [x] 登录失败锁定（LoginAttemptServiceImpl.java：Redis 版，连续 5 次失败锁定 15 分钟，Redis 不可用自动降级，TTL 防永久锁定）
- [ ] 并发登录限制（可选）— 未实现
- [ ] 添加 AccessDeniedException 处理 — 未实现

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

### 11.1 VO 类定义 ✅
- [x] 定义 AiSummaryVO（总结响应）
- [x] 定义 AiQaResponseVO（问答响应）
- [x] 定义 AiQuizResponseVO（出题响应）
- [x] 定义 AiPlanResponseVO（学习计划响应）
- [x] 定义 WrongQuestionListVO（错题列表响应）
- [x] 额外：UserVO、MaterialVO、MaterialDetailVO（共 8 个 VO 类已实现）

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
第一阶段（2-3 周）：完成 Phase 7 剩余主链路接入与质量评估
  → 7.3 资料向量索引 → 7.5 接入 AiQaService → 7.4/7.5 单元测试 → 7.6 评估

第二阶段（2 周）：Phase 8 + Phase 9
  → 会话持久化 + 安全加固

第三阶段（2 周）：Phase 10 + Phase 11
  → 单元测试 + 错误处理优化

第四阶段（2 周）：Phase 12 + Phase 13
  → 前端重构 + 暗色模式

第五阶段（按需）：Phase 14 - Phase 18
  → 功能扩展与性能优化
```

## 里程碑

| 里程碑 | 目标 | 预计时间 |
|--------|------|----------|
| M1 | 资料索引接通 + RAG 混合检索主链路上线 | 第 3 周末 |
| M2 | 会话持久化 + 安全加固 | 第 6 周末 |
| M3 | 测试覆盖 + 错误处理优化 | 第 8 周末 |
| M4 | 前端重构 + 主题系统 | 第 10 周末 |
| M5 | 数据分析 + 工程化部署 | 第 12 周末 |

---

## Phase A3-3: Competition model stack migration (2026-07-18)

> Goal: make the Docker demo stack cloud-first and low-cost while preserving optional local Ollama tools for future development.

- [x] Switch global OpenAI-compatible chat defaults from MiMo to iFlytek Spark Lite.
- [x] Switch router/expert Agent defaults to `AI_BASE_URL` + `AI_CHAT_MODEL=lite`.
- [x] Switch uploaded image understanding Agent from local `qwen2.5vl` to Alibaba Cloud Bailian `qwen-vl-plus`.
- [x] Keep `agent-qwen-tools.yml` as optional local Ollama configuration, outside the Docker-required path.
- [x] Update Docker Compose and `.env.example` for Spark Lite, Bailian Embedding, Bailian VL, and iFlytek TTS/HiDream.

## Phase Cleanup-1: Safe project cleanup (2026-07-18)

> Goal: remove generated, temporary, or stale files without changing application behavior.

- [x] Back up human-authored cleanup candidates before deletion.
- [x] Remove stale unreferenced dashboard variant and obsolete pgvector trust config.
- [x] Remove temporary notes, accidental Windows `nul` file, local review reports, Playwright snapshots, logs, and generated build outputs.
- [x] Update `.gitignore` so root build artifacts and cleanup report/temp files do not reappear.
- [x] Verify frontend production build after cleanup.

## Phase PPT-1: A3 competition deck generation (2026-07-18)

> Goal: align the competition PPT specification with the current implementation and produce a first deliverable deck.

- [x] Optimize `docs/competition-ppt-generation-spec.md` with current project state, capability boundaries, screenshot strategy, and delivery requirements.
- [x] Generate a 20-slide A3 competition presentation with editable diagrams, screenshot placeholders, and speaker notes.
- [x] Export final deck to `outputs/ai-study-assistant-a3-competition.pptx`.
- [x] Render and QA the deck; confirmed 20 slides and no overflow.

## Phase PPT-2: A3 competition deck visual enhancement (2026-07-18)

> Goal: upgrade the first competition deck into a more polished, visually memorable roadshow presentation while preserving its content boundaries and editability.

- [x] Preserve the original PPT and export a separate visual-enhanced version.
- [x] Add a cinematic AI-education hero visual for the cover and closing slide.
- [x] Rebuild the 20-slide visual system with stage-style headers, richer cards, browser-style screenshot frames, and stronger process diagrams.
- [x] Strengthen the core resource-delivery slide with a seven-category resource package composition.
- [x] Render and inspect all 20 slides; confirm no overflow or unintended clipping.

## Phase Docker-2: System knowledge bank mount (2026-07-18)

> Goal: make the system knowledge bank visible inside Docker containers for demo and teammate deployment.

- [x] Mount `backend/src/main/resources/knowledge-bank` into the backend container as `/app/resources/knowledge-bank`.
- [x] Set `AI_KNOWLEDGE_BANK_ROOT=/app/resources/knowledge-bank` for the backend service.

## Phase A3-4: Multimodal asset hotfix (2026-07-18)

> Goal: improve demo stability for podcast narration and HiDream image generation.

- [x] Harden podcast script generation with system-prompt isolation, prompt-leak cleanup, and summary-based fallback narration.
- [x] Fix HiDream result parsing so task ids and other non-image strings are not decoded as image base64.
- [x] Add focused regression tests for podcast prompt-leak fallback and HiDream image payload extraction.

## Phase Docker-3: Docker demo validation and HiDream stability (2026-07-18)

> Goal: ensure the teammate/demo Docker stack starts successfully and core AI/multimodal flows work with cloud providers.

- [x] Verify `docker-compose up -d --build` starts PostgreSQL, Redis, backend, and frontend.
- [x] Verify frontend `/` and backend `/api/actuator/health` are reachable.
- [x] Verify Docker knowledge-bank mount scans `/app/resources/knowledge-bank` successfully.
- [x] Verify upload, material processing, summary task, RAG QA, resource package, podcast audio, and HiDream image asset flows.
- [x] Persist image original prompt before provider calls so failed image assets can be retried.
- [x] Add HiDream HTTP retry handling for transient empty-header connection failures.
- [x] Increase default HiDream timeout to 180 seconds for demo reliability.
- [x] Clear stale error fields after successful asset retry.

## Phase Cleanup-2: Delivery readiness cleanup (2026-07-18)

> Goal: make the project cleaner for teammate handoff while preserving local-only files that may still be useful on this machine.

- [x] Ignore local Claude permission settings and local backup archives instead of deleting them.
- [x] Remove unused `AgentConfigLoader` Phase 4 stub; real YAML loading is handled by `AgentClientFactory`.
- [x] Remove noisy `System.out.println` from the knowledge-bank loader test.
- [x] Fix stale `agent-qwen-tools.yml` type from `GENERAL` to `ROUTER` so startup logs stay clean.
- [x] Rebuild backend/frontend Docker images and verify Compose startup, frontend `/`, backend `/api/actuator/health`, and knowledge-bank scan.

## Phase UX-5: Resource export, chat cleanup, and prompt sharpening (2026-07-18)

> Goal: polish competition-facing workflows before handoff and make expert Agents more distinct.

- [x] Replace resource workshop JSON export with one-click browser PDF export built from generated documents, mind map, quiz, learning path, review notes, and successful image assets; audio is excluded.
- [x] Remove unused AI chat model selector and web search toggle from the chat header.
- [x] Move dark/light mode switching from AI chat's local sidebar into the global application sidebar.
- [x] Widen the resource workshop configuration column and make dense controls wrap or scroll inside the card.
- [x] Strengthen router, civil, graduate, and general QA prompts with clearer role boundaries, answer methods, source discipline, and no internal file-structure leakage.
- [x] Verify frontend build, backend Docker build, frontend Docker build, Compose startup, frontend `/`, backend `/api/actuator/health`, and knowledge-bank scan.

## Phase Docs-4: Public demo README (2026-07-18)

> Goal: make the README suitable for judges, viewers, and demo users while keeping startup commands clear.

- [x] Rewrite `README.md` into a public-facing project overview and quick-start guide.
- [x] Highlight competition value, feature scope, technical stack, and multimodal/multi-agent capabilities.
- [x] Keep Docker startup, status, logs, stop, and access URL commands concise.
- [x] Include required `.env` variables for iFlytek Spark, iFlytek TTS/HiDream, and Alibaba Cloud Bailian.

## Phase UX-6: Resource workshop learner-facing copy (2026-07-18)

> Goal: remove competition-facing copy from the in-app resource package panel and keep the interface natural for learners.

- [x] Replace visible "演示亮点" and "质量与安全说明" labels with learner-facing copy while keeping "多智能体分工" as the Agent section title.
- [x] Rename generated "多模态资源脚本包" titles to "多模态资源".
- [x] Replace "复制演示摘要" and fallback export names with "资源摘要" wording.
- [x] Rewrite generated resource notes and safety checks so new packages describe learning value instead of judging/demo intent.
- [x] Verify frontend production build after the copy update.
