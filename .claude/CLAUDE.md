# CLAUDE.md

# Project Name

AI Study Assistant

# Project Description

一个基于 Spring Boot 3 + Spring AI + Vue3 的 AI 智能学习助手系统。

系统支持：
- 学习资料上传与管理；
- AI 知识总结；
- 基于 RAG 的文档问答；
- AI 自动出题与自动判分；
- 错题本管理；
- AI 个性化学习计划生成。

项目采用前后端分离架构，遵循 Spec-Driven Development（规格驱动开发）模式。

---

# Tech Stack

## Backend

- Java 21
- Spring Boot 3.x
- Spring AI 1.0.0（正式版）
- MyBatis-Plus
- Spring Web
- Spring Validation
- Spring Security + JWT Authentication
- Maven

## Frontend

- Vue3
- Vite
- Element Plus
- Pinia
- Axios
- Vue Router

## Database

- MySQL 8.0

## AI

- Spring AI ChatClient
- DeepSeek API（兼容 OpenAI API）
- RAG（Embedding + Vector Store）
- Embedding Model：bge-small-zh（本地部署）
- Hybrid Search：Vector Search + BM25
- Optional Reranking

---

# Project Structure

```text
.claude/
├── CLAUDE.md
├── task-list.md
└── prompts/

docs/
└── spec.md

backend/
frontend/
sql/
```

---

# Development Principles

- 遵循前后端分离架构。
- 后端采用 Controller → Service → Mapper 三层结构。
- 所有接口遵循 RESTful 设计规范。
- 所有接口统一使用 `Result<T>` 返回。
- 数据库操作统一使用 MyBatis-Plus。
- DTO、VO、Entity 必须分离。
- 禁止在 Controller 中编写业务逻辑。
- AI 相关代码统一放在 `ai` 包下。
- 编写代码优先保证可读性、可维护性和模块化。
- 优先使用 Lombok 减少模板代码。
- 优先进行增量开发（Incremental Development），避免一次性大规模修改。

---

# API Response Format

```java
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
}
```

## Success

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

## Error

```json
{
  "code": 500,
  "message": "error",
  "data": null
}
```

---

# Coding Style

## Naming Convention

- Class：PascalCase
- Method：camelCase
- Variable：camelCase
- Constant：UPPER_SNAKE_CASE

## Coding Rules

- 每个 Service 接口必须对应一个实现类。
- 所有公共方法必须添加 JavaDoc 注释。
- 优先使用 Lombok 减少模板代码。
- 保持单一职责原则（SRP）。
- 避免重复代码，必要时提取公共组件。
- 不要过度设计，不提前实现未来可能使用的功能（YAGNI）。

---

# AI Module Rules

## 1. 文档总结（AI Summary）

- 使用 Spring AI ChatClient。
- 输入学习资料内容。
- 输出 Markdown 格式知识总结。
- 总结内容应包含：
  - 核心知识点；
  - 重要概念；
  - 关键公式（如有）；
  - 学习建议。

## 2. 文档问答（RAG QA）

采用标准 RAG 工作流：

1. 检索文档切片；
2. 进行向量检索；
3. 结合 BM25 关键词检索；
4. 获取 Top-K（默认 5）最相关内容；
5. （可选）执行 Reranking；
6. 拼接 Prompt；
7. 调用大模型生成最终回答。

要求：
- 默认 Top-K = 5；
- 支持多轮对话；
- 优先基于知识库回答；
- 如果知识库不存在答案，应明确说明，而不是编造内容。

## 3. AI 自动出题

根据学习资料自动生成：

- 5 道单选题；
- 3 道判断题；
- 2 道简答题。

每道题必须包含：
- 题目；
- 标准答案；
- 答案解析。

支持：
- 用户作答；
- 自动判分；
- 错题自动加入错题本。

## 4. AI 学习计划

根据：
- 学习目标；
- 考试日期；
- 每日可用学习时间；

自动生成个性化学习计划。

---

# Frontend Rules

- 页面组件统一放在 `views/`。
- 通用组件统一放在 `components/`。
- API 请求统一封装在 `api/` 模块。
- 状态管理统一使用 Pinia。
- UI 框架统一使用 Element Plus。
- Markdown 渲染统一使用 `markdown-it`（推荐）或 `marked`。
- 页面应包含必要的 Loading、异常提示和空状态展示。

---

# Modification Rules

为了保证项目稳定性，Claude 在开发过程中必须遵守：

- 不修改当前任务无关模块。
- 不进行大规模重构，除非用户明确要求。
- 不擅自修改数据库结构。
- 不擅自修改已有接口输入输出格式。
- 保持向后兼容（Backward Compatibility）。
- 优先进行局部修改，而不是整体重写。
- 如果需求存在歧义，优先遵循 `docs/spec.md`。

---

# Database Rules

涉及数据库修改时，必须同步更新：

- Entity；
- DTO / VO（如受影响）；
- Mapper；
- SQL 初始化脚本；
- 相关开发文档。

未经明确要求：
- 不新增无意义字段；
- 不删除已有字段；
- 不修改已有表结构。

---

# Documentation Rules

实现新功能时：

- 新增接口必须同步更新 API 文档。
- 完成任务后同步更新 `.claude/task-list.md`。
- 保证实现与 `docs/spec.md` 保持一致。
- 如果实现方案与 Spec 存在冲突，应优先遵循 Spec，并说明原因。

---

# Development Workflow

在实现任何功能之前，Claude 应遵循以下流程：

1. 阅读 `.claude/CLAUDE.md`；
2. 阅读 `docs/spec.md`；
3. 阅读 `.claude/task-list.md`；
4. 找到当前未完成的任务；
5. 分析并设计实现方案；
6. 编写完整、可直接运行的代码；
7. 添加必要注释和参数校验；
8. 验证代码能够正常编译和运行；
9. **对本次变更进行代码审查（见 Code Review Rules）**；
10. 完成任务后更新 `.claude/task-list.md`；
11. 不修改当前任务无关模块。

---

# Code Review Rules

**每完成一个模块/任务后，或用户要求审查代码时，必须：**

1. Read 并执行 `.claude/prompts/code-review.md` — 其中包含完整的 6 维审查 Checklist 和修复工作流
2. 按 Checklist 逐项审查本次变更（或全局代码），不得跳过维度
3. 对发现的 🔴 严重问题必须修复；🟡 建议问题择机修复
4. 修复后再次扫描确认无新问题引入

**禁止**：口头审查后不修复、审查维度缩水、修复时进行无关重构。

---

# Prompt Workflow

项目使用 `.claude/prompts/` 目录中的 Prompt 模板辅助开发。

常用工作流：

| 场景 | 使用 Prompt |
|------|-------------|
| 开始一天开发 | continue-work.md |
| 开始新功能 | implement-feature.md |
| 生成后端接口 | generate-api.md |
| 生成前端页面 | generate-frontend.md |
| 修复 Bug | bug-fix.md |
| 代码审查 | code-review.md |
| 安全重构 | refactor.md |
| 生成测试代码 | generate-test.md |
| 项目巡检 | project-status.md |

Claude 在执行任务时，可以参考对应 Prompt 的工作模式。

---

# Spec-Driven Development

项目采用 Spec 驱动开发模式。

开发实现必须以以下文档为最高优先级：

- `docs/spec.md`

该文档定义了：
- 业务需求；
- 功能模块；
- 数据模型；
- API 契约；
- AI 工作流；
- 行为约束；
- 验收标准。

如果实现方案与文档存在冲突，应优先遵循 `docs/spec.md`。

---

# Project Development Rules

- 遵循 MVP（Minimum Viable Product）优先原则。
- 一次只完成一个任务。
- 每次提交只聚焦一个功能点。
- 不提前优化，不提前扩展需求。
- 不随意引入新的框架或依赖。
- 优先保证代码可维护性，而不是追求复杂技巧。
- 发现需求不明确时，优先查阅 `docs/spec.md`，必要时向用户确认。

---

# Goal

生成结构清晰、模块解耦、易于维护、符合企业级开发规范的代码。

优先保证：
1. 正确性；
2. 可维护性；
3. 可扩展性；
4. 与项目 Spec 保持一致。

目标是构建一个高质量、可持续迭代的 AI 智能学习助手项目。

## Prompt Selection Rules

当用户以意图描述而非显式指定 Prompt 文件时，Claude 应自动从 `.claude/prompts/` 选择最合适的模板。

### 选择映射表

| 用户意图（关键词匹配） | 使用 Prompt | 说明 |
|------------------------|------------|------|
| "开始新任务"、"开始开发"、"新功能" | `start-task.md` | 先阅读 CLAUDE.md + spec.md + task-list.md 再开始 |
| "继续开发"、"继续工作"、"继续" | `continue-work.md` | 总结进度 → 找下一个未完成任务 → 开始实现 |
| "实现功能"、"开发功能"、"添加功能" | `implement-feature.md` | 分析需求 → 设计方案 → 编写代码 |
| "生成接口"、"创建 API"、"后端接口" | `generate-api.md` | 按 Controller → Service → Mapper 生成后端接口 |
| "生成页面"、"创建页面"、"前端页面" | `generate-frontend.md` | 按 views/api/stores 生成前端页面 |
| "修复 Bug"、"修 Bug"、"报错"、"异常" | `bug-fix.md` | 分析 → 定位 → 最小修改 → 验证 |
| "审查代码"、"检查代码"、"Code Review" | `code-review.md` | 6 维审查 → 发现问题 → 修复 → 验证（见 Code Review Rules） |
| "重构"、"优化结构" | `refactor.md` | 安全重构，不改变外部行为 |
| "生成测试"、"写测试"、"单元测试" | `generate-test.md` | 生成 JUnit / Vue Test Utils 测试代码 |
| "项目检查"、"完成度"、"巡检" | `project-status.md` | 对照 spec 和 task-list 检查项目整体进度 |

### 复合意图处理

当用户意图涉及多个 Prompt（如 "审查代码然后修复"），优先使用能力更强的 Prompt：
- **审查 + 修复** → `code-review.md`（已内置审查→修复→验证完整闭环）
- **继续开发 + 新功能** → `implement-feature.md`（先执行 continue-work 扫描，再 design → implement）

### 规则

1. 匹配到 Prompt 后，应在生成回复前**先 Read 该 Prompt 文件**
2. 如果用户意图不在表中，不强行匹配 — 直接按 CLAUDE.md 基础规则执行
3. 匹配到的 Prompt 中如包含"先阅读文档"指令（如 start-task.md），必须执行

## UI Development Rules

Frontend development must follow:

1. docs/spec.md
2. docs/ui-design-spec.md

Do not invent new styles.

All pages must follow the Design System defined in ui-design-spec.md.

Maintain consistent spacing, typography, colors, layout and interaction across the entire application.