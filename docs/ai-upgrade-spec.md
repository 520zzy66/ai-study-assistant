# AI 总结 & 出题功能升级 Spec

> 创建日期：2026-07-08
> 状态：待实施

## Context

当前 AI 总结和出题功能通过 `AiClient.chat()` 直接调用 MiMo 模型，存在以下局限：
1. 总结生成没有流式输出，用户体验差（等待 loading → 一次性显示）
2. 总结和思维导图/知识图谱是独立功能，没有联动
3. 出题仅支持单选、判断、简答三种题型，缺少填空、多选、数学题
4. 难度控制（easy/medium/hard）效果不明显
5. 错题本只有列表展示，缺少重练、统计、导出功能

本次升级将：
- 引入 Agent 编排，用 Agent 调用 QwenTools（思维导图/知识图谱）替代直接后端调用
- 总结页面支持 SSE 流式输出 + 思维导图并行生成
- 出题新增填空、多选、数学填空三种题型
- 错题本增加重练、统计图表、PDF 导出

---

## Phase 1: 总结流式输出 + Agent 编排

### 目标
- 总结页面支持 SSE 流式输出（打字机效果）
- 总结完成后自动展示思维导图
- 使用 Agent 编排：MiMo 生成总结 + QwenTools 生成思维导图

### 后端改动

#### 1.1 新增总结 Agent 配置
**文件**: `backend/src/main/resources/agents/agent-summary.yml`

```yaml
id: summary
name: "总结助手"
description: "生成知识总结，可调用思维导图工具"
type: GENERAL
order: 5
model:
  base-url: ${AI_BASE_URL}
  api-key: ${AI_API_KEY}
  model: ${AI_MODEL:-MiMo}
  temperature: 0.4
system-prompt: prompts/summary-agent.txt
```

**文件**: `backend/src/main/resources/prompts/summary-agent.txt`

System prompt 指导 Agent：
1. 先生成知识总结（Markdown 格式）
2. 调用 `generateMindMap` 工具生成思维导图
3. 最终返回格式：总结文字 + `[MIND_MAP_JSON]{...}[/MIND_MAP_JSON]`

#### 1.2 改造 AiSummaryService
**文件**: `backend/src/main/java/com/study/ai/service/AiSummaryService.java`

新增方法：
- `generateSummaryWithMindMap(Long materialId, boolean force)` → 返回 `Map<String, Object>` 包含 `summary` 和 `mindMap`
- 使用 `AgentClientFactory.getOrCreate("summary")` 获取 ChatClient
- 注入 `QwenTools`：`.tools(qwenTools)`
- Agent 自动决定何时调用 `generateMindMap` tool
- Spring AI 自动执行 tool 方法，结果回传给 Agent

#### 1.3 新增流式总结接口
**文件**: `backend/src/main/java/com/study/controller/AiSummaryController.java`

新增端点：
- `GET /ai/summary/stream/{materialId}` → `Flux<String>`
- 使用 `ChatClient.prompt().stream().content()` 流式输出
- 流式结束后，前端再请求思维导图

新增端点：
- `GET /ai/summary/mindmap/{materialId}` → `Result<Map>`
- 返回已生成的思维导图 JSON（从数据库读取）

#### 1.4 数据库：存储思维导图
**表**: `learning_material` 新增字段 `mind_map TEXT`（存储思维导图 JSON）

或复用现有 `AiChatHistory` 表，chatType 新增 `summary_mindmap` 类型。

**推荐**：直接在 `learning_material` 表新增 `mind_map` 字段，与 `summary` 字段对称。

### 前端改动

#### 1.5 新增流式 API
**文件**: `frontend/src/api/ai.js`

新增：
```js
export function generateSummaryStream(materialId, onChunk, onDone, onError) {
  // 使用 fetch + ReadableStream，参考 askQuestionStream() 的实现模式
}
```

#### 1.6 改造 AiSummary.vue
**文件**: `frontend/src/views/AiSummary.vue`

改造点：
- "生成总结"按钮触发后：
  1. 先调用 `generateSummaryStream()` 流式渲染总结（打字机效果）
  2. 流式完成后，调用 `getMindMap(materialId)` 获取思维导图
  3. 总结下方新增思维导图展示区域
- 思维导图展示：复用 SVG 或简单树形结构渲染（不引入 echarts）
- 保留现有的 loading/进度条作为 fallback（网络不支持 SSE 时）

### 验证方式
1. 启动后端，确保 Ollama qwen2.5:7b 和 MiMo API 可用
2. 上传一份学习资料
3. 进入 AI 总结页面，选择资料，点击"生成总结"
4. **验证**：总结文字逐字出现（打字机效果）
5. **验证**：总结完成后，下方自动展示思维导图树形结构
6. **验证**：刷新页面后，总结和思维导图都持久化可查看

---

## Phase 2: 出题新增题型 + 难度优化

### 目标
- 新增填空题、多选题、数学填空题
- 优化难度 Prompt，让 easy/medium/hard 真正有区分
- 数学题支持数值容差判分（0.0001）

### 后端改动

#### 2.1 扩展 PromptTemplates
**文件**: `backend/src/main/java/com/study/ai/prompt/PromptTemplates.java`

修改 `QUIZ_TEMPLATE`：
- 新增题型：`fill_blank`（填空）、`multi_choice`（多选）、`math_fill`（数学填空）
- 优化难度描述：
  - `easy`: 基础概念记忆、直接应用
  - `medium`: 概念理解、简单分析、需要 1-2 步推理
  - `hard`: 综合分析、多步推理、跨知识点应用
- 数学题格式：`{"type": "math_fill", "question": "求∫₀¹ x² dx = ____", "answer": "1/3", "tolerance": 0.0001}`

#### 2.2 新增判分逻辑
**文件**: `backend/src/main/java/com/study/ai/service/AiQuizService.java`

新增方法：
- `gradeFillBlank(question, referenceAnswer, userAnswer)` — 填空题判分
  - 先精确匹配（trim + ignoreCase）
  - 失败则 AI 语义判分（类似简答题）
- `gradeMultiChoice(question, referenceAnswer, userAnswer)` — 多选题判分
  - 解析参考答案（如 "A,C,D"）和用户答案
  - 集合相等判断，全对才得分
- `gradeMathFill(question, referenceAnswer, userAnswer, tolerance)` — 数学题判分
  - 先精确匹配字符串
  - 失败后尝试数值解析（支持分数、百分数、根号等）
  - 数值差 < 0.0001 视为正确
  - 都失败则 AI 语义判分

**数学表达式解析**：
- 使用 Java 内置 `ScriptEngine`（Nashorn）或手动解析
- 支持：整数、小数、分数（`1/3`）、百分数（`50%`）、根号（`√2`）、π、e
- 不引入外部依赖，保持轻量

#### 2.3 修改提交答案逻辑
**文件**: `backend/src/main/java/com/study/ai/service/AiQuizService.java`

修改 `submitAnswers()`：
- 扩展判分分支，新增 `fill_blank`、`multi_choice`、`math_fill` 的处理
- 修改 `reAnswer()` 方法支持新题型

#### 2.4 修改前端展示
**文件**: `frontend/src/views/AiQuiz.vue`

新增 UI：
- **填空题**：显示题干 + input 输入框
- **多选题**：显示选项，每个选项带 checkbox（而非 radio）
- **数学填空题**：显示题干 + input 输入框（带 placeholder "输入答案"）
- 答题状态网格：适配新题型的颜色逻辑

#### 2.5 扩展 DTO
**文件**: `backend/src/main/java/com/study/dto/request/GenerateQuizRequest.java`

新增字段：
- `fillBlankCount`（填空题数量，默认 0）
- `multiChoiceCount`（多选题数量，默认 0）
- `mathFillCount`（数学填空题数量，默认 0）

### 验证方式
1. 进入 AI 出题页面，设置各题型数量（如 2 填空 + 2 多选 + 2 数学）
2. **验证**：生成的题目包含所有请求的题型
3. **验证**：填空题可以输入文本提交
4. **验证**：多选题可以勾选多个选项，全对才得分
5. **验证**：数学题输入 `1/3` 和 `0.3333` 都能判对（容差 0.0001）
6. **验证**：设置 easy/medium/hard 难度后，题目难度有明显差异

---

## Phase 3: 错题本增强（重练 + 统计）

### 目标
- 错题重练：智能推荐（优先高错误率 + 最近错的）
- 错题统计：按知识点的错误率柱状图 + 正确率趋势线图 + 错题标签云
- 时间范围可选（7天/30天/全部/自定义）

### 后端改动

#### 3.1 错题重练接口
**文件**: `backend/src/main/java/com/study/controller/AiQuizController.java`

新增端点：
- `GET /ai/quiz/wrong/repractice?count=10` → `Result<List<AiQuestionBank>>`
- 智能推荐逻辑：
  1. 查询未掌握错题
  2. 按 `wrong_count DESC, last_wrong_time DESC` 排序
  3. 取前 N 题返回（复用现有题目数据，不重新生成）

#### 3.2 错题统计接口
**文件**: `backend/src/main/java/com/study/controller/AiQuizController.java`

新增端点：
- `GET /ai/quiz/wrong/stats?startDate=&endDate=` → `Result<Map>`

返回数据结构：
```json
{
  "totalWrong": 42,
  "masteredCount": 15,
  "masteryRate": 35.7,
  "byKnowledgePoint": [
    {"name": "数据结构", "wrongCount": 12, "totalCount": 30, "errorRate": 40.0}
  ],
  "byDate": [
    {"date": "2026-07-01", "wrongCount": 3, "correctCount": 7}
  ],
  "tagCloud": [
    {"tag": "排序算法", "count": 8},
    {"tag": "二叉树", "count": 5}
  ]
}
```

**知识点提取**：从题目内容中提取关键词（可用简单分词或 AI 提取），或按资料名称分组。
**日期统计**：按 `AiQuizRecord.createTime` 分组统计每日正确/错误数。
**标签云**：按错误次数排序的题目关键词。

#### 3.3 修改 WrongQuestion.vue
**文件**: `frontend/src/views/WrongQuestion.vue`

新增功能：
- **重练按钮**：点击后跳转到 AiQuiz.vue 并携带 `repractice=true&from=wrong` 参数
- **统计面板**：错题列表上方新增可折叠的统计区域
  - 柱状图：按知识点的错误率（手写 SVG，参考 AreaChart.vue 模式）
  - 线图：正确率趋势（复用 AreaChart.vue）
  - 标签云：错误高频关键词（手写 SVG，字号按错误次数缩放）
- **时间筛选**：统计面板新增日期范围选择器

#### 3.4 改造 AiQuiz.vue 支持重练模式
**文件**: `frontend/src/views/AiQuiz.vue`

新增逻辑：
- 检测 `route.query.repractice` 参数
- 重练模式下：
  - 隐藏配置面板（题型/难度设置）
  - 直接从 `/ai/quiz/wrong/repractice` 加载题目
  - 提交判分逻辑复用现有 `submitAnswers`
  - 答对的题自动标记已掌握

### 验证方式
1. 先做一套题，制造一些错题
2. 进入错题本页面
3. **验证**：统计面板显示柱状图、趋势图、标签云
4. **验证**：时间范围筛选生效
5. **验证**：点击"错题重练"后跳转到做题页面，加载错题
6. **验证**：重练答对后，错题本中该题标记为已掌握

---

## Phase 4: 错题导出 PDF

### 目标
- 错题本支持导出 PDF
- PDF 包含：题目 + 用户错误答案 + 正确答案 + 解析

### 后端改动

#### 4.1 PDF 生成服务
**文件**: `backend/src/main/java/com/study/service/WrongQuestionExportService.java`

技术选型：**OpenPDF**（`com.github.librepdf:openpdf:2.0.3`），LGPL 友好，支持中文。

新增方法：
- `exportPdf(Long userId, Long materialId, Boolean isMastered)` → `byte[]`
- 生成内容：
  - 标题：错题本导出 - 2026-07-08
  - 每道题：题号、题型、题目、用户答案（红色）、正确答案（绿色）、解析
  - 分页处理

#### 4.2 新增导出接口
**文件**: `backend/src/main/java/com/study/controller/AiQuizController.java`

新增端点：
- `GET /ai/quiz/wrong/export/pdf?materialId=&isMastered=` → `application/pdf`
- 返回 `ResponseEntity<byte[]>`，带 `Content-Disposition: attachment` header

#### 4.3 新增 Maven 依赖
**文件**: `backend/pom.xml`

```xml
<dependency>
    <groupId>com.github.librepdf</groupId>
    <artifactId>openpdf</artifactId>
    <version>2.0.3</version>
</dependency>
```

### 前端改动

#### 4.4 导出按钮
**文件**: `frontend/src/views/WrongQuestion.vue`

新增：
- 工具栏新增"导出 PDF"按钮
- 点击后调用 `/ai/quiz/wrong/export/pdf`，浏览器下载文件
- 支持按当前筛选条件导出（已选的资料/掌握状态）

### 验证方式
1. 错题本有数据的情况下，点击"导出 PDF"
2. **验证**：浏览器下载 PDF 文件
3. **验证**：PDF 包含所有错题，格式正确
4. **验证**：用户答案红色、正确答案绿色、解析正常显示
5. **验证**：中文正常渲染（无乱码）

---

## 实施顺序与依赖关系

```
Phase 1 (总结流式 + Agent编排)
  ├── 后端: agent-summary.yml + AiSummaryService改造 + 流式接口 + DB字段
  └── 前端: 流式API + AiSummary.vue改造
  验证: 总结流式输出 + 思维导图展示

Phase 2 (出题新题型) — 可与 Phase 1 并行
  ├── 后端: PromptTemplates扩展 + 判分逻辑 + DTO扩展
  └── 前端: AiQuiz.vue 新题型UI
  验证: 填空/多选/数学题生成+作答+判分

Phase 3 (错题本增强) — 依赖 Phase 2（新题型数据）
  ├── 后端: 重练接口 + 统计接口
  └── 前端: WrongQuestion.vue 统计面板 + 重练跳转
  验证: 统计图表 + 重练流程

Phase 4 (PDF导出) — 可与 Phase 3 并行
  ├── 后端: OpenPDF依赖 + 导出服务 + 导出接口
  └── 前端: 导出按钮
  验证: PDF下载+内容正确
```

---

## 技术决策记录

| 决策 | 选择 | 原因 |
|------|------|------|
| 流式方案 | SSE (Flux\<String\>) | 项目已有成熟模式（AiClient.chatStream），Spring Boot 原生支持 |
| Agent 编排方式 | ChatClient.prompt().tools() | 项目现有模式，不引入额外框架复杂度 |
| 思维导图存储 | learning_material.mind_map 字段 | 与 summary 字段对称，查询简单 |
| 数学表达式解析 | Java 内置 ScriptEngine + 手动解析 | 轻量，不引入外部依赖 |
| 图表库 | 手写 SVG | 项目现有模式，保持一致性 |
| PDF 库 | OpenPDF (LGPL) | 协议友好，支持中文，轻量 |
| 错题知识点提取 | 按资料名称分组 | 简单可靠，不依赖 NLP |

---

## 文件变更清单

### 新增文件
| 文件 | Phase | 说明 |
|------|-------|------|
| `backend/src/main/resources/agents/agent-summary.yml` | 1 | 总结 Agent 配置 |
| `backend/src/main/resources/prompts/summary-agent.txt` | 1 | 总结 Agent System Prompt |
| `backend/src/main/java/com/study/service/WrongQuestionExportService.java` | 4 | PDF 导出服务 |
| `sql/migration/V20260708__add_mind_map.sql` | 1 | 数据库迁移脚本 |

### 修改文件
| 文件 | Phase | 说明 |
|------|-------|------|
| `backend/src/main/java/com/study/ai/service/AiSummaryService.java` | 1 | 新增流式+Agent编排方法 |
| `backend/src/main/java/com/study/controller/AiSummaryController.java` | 1 | 新增流式端点 |
| `backend/src/main/java/com/study/ai/prompt/PromptTemplates.java` | 2 | 扩展出题模板 |
| `backend/src/main/java/com/study/ai/service/AiQuizService.java` | 2 | 新增判分逻辑 |
| `backend/src/main/java/com/study/dto/request/GenerateQuizRequest.java` | 2 | 新增题型数量字段 |
| `backend/src/main/java/com/study/controller/AiQuizController.java` | 3,4 | 重练+统计+导出端点 |
| `backend/src/main/java/com/study/entity/LearningMaterial.java` | 1 | 新增 mindMap 字段 |
| `backend/pom.xml` | 4 | OpenPDF 依赖 |
| `frontend/src/api/ai.js` | 1 | 流式总结 API |
| `frontend/src/api/quiz.js` | 3,4 | 重练+统计+导出 API |
| `frontend/src/views/AiSummary.vue` | 1 | 流式渲染+思维导图展示 |
| `frontend/src/views/AiQuiz.vue` | 2,3 | 新题型UI+重练模式 |
| `frontend/src/views/WrongQuestion.vue` | 3,4 | 统计面板+导出按钮 |
