# AI 工作流实现详解

> **面向对象**：外行小白
> **目标**：让你完全理解这个 AI 工作流是怎么搭建的、每一行代码在干什么、为什么要这样设计

---

## 一、先讲大白话：这个系统在干什么？

想象你是一个学生，打开一个学习助手网站，问了一个问题：

> "什么是申论？"

系统收到这个问题后，要做一系列事情：

1. **理解你的问题** — 判断这是简单问题还是复杂问题
2. **查找你是谁** — 读取你的个人画像、学习记忆、历史对话
3. **决定谁来回答** — 是直接回答，还是交给专业领域的 AI 专家
4. **生成回答** — 给出一个有理有据的答案
5. **记住这次对话** — 把对话存起来，下次能回忆

这整个过程，就是**工作流**。

---

## 二、核心概念：什么是 StateGraph（状态图）？

### 2.1 用快递站来比喻

想象一个快递站，包裹（用户问题）进来后要经过几个工位：

```
包裹进来 → 【分拣工位】→ 轻件 → 【直接派送工位】→ 送达
                    ↓
                   重件 → 【专业处理工位】→ 送达
```

- **包裹** = 用户的问题
- **分拣工位** = GeneralNode（路由节点）
- **直接派送工位** = LocalAnswerNode（简单回答节点）
- **专业处理工位** = ExpertAgentNode（专家回答节点）
- **送达** = END（结束）

在代码中，这叫 **StateGraph（状态图）**：
- **Graph** = 图，就是上面那个流程图
- **State** = 状态，包裹上贴的信息（用户ID、问题内容、路由结果等）
- **Node** = 节点，每个工位
- **Edge** = 边，工位之间的连线
- **Conditional Edge** = 条件边，根据条件走不同的路

### 2.2 关键类说明

| 类名 | 作用 | 比喻 |
|------|------|------|
| `StateGraph` | 定义图的结构（有哪些节点、怎么连线） | 快递站的布局图纸 |
| `OverAllState` | 所有节点共享的数据容器 | 包裹上的信息贴纸 |
| `NodeAction` | 节点要执行的操作接口 | 每个工位的工作手册 |
| `CompiledGraph` | 编译后的图，可以执行 | 建好的快递站，可以开工了 |

---

## 三、图是怎么搭建的？（逐行代码解析）

### 3.1 搭图入口：WorkflowGraphService.buildGraph()

```java
private StateGraph buildGraph() throws Exception {
    return new StateGraph()
        // 第一步：添加三个节点
        .addNode("general", AsyncNodeAction.node_async(generalNode))
        .addNode("localAnswer", AsyncNodeAction.node_async(localAnswerNode))
        .addNode("expertAgent", AsyncNodeAction.node_async(expertAgentNode))

        // 第二步：添加连线
        .addEdge(START, "general")           // 开始 → 分拣工位
        .addConditionalEdges("general",       // 分拣工位 → 根据条件走
            AsyncEdgeAction.edge_async(this::routeDecision),
            Map.of("local", "localAnswer",   // 条件"local" → 直接派送
                   "expert", "expertAgent"))  // 条件"expert" → 专业处理
        .addEdge("localAnswer", END)          // 直接派送 → 结束
        .addEdge("expertAgent", END);         // 专业处理 → 结束
}
```

**逐行解释**：

| 代码 | 做了什么 | 为什么 |
|------|----------|--------|
| `new StateGraph()` | 创建一个空的图 | 就像画一张空白流程图 |
| `.addNode("general", ...)` | 添加"分拣工位"节点 | `AsyncNodeAction.node_async()` 把同步的 `NodeAction` 包装成异步的，这是框架要求 |
| `.addEdge(START, "general")` | 从起点画一条线到分拣工位 | 所有问题先进分拣 |
| `.addConditionalEdges("general", ...)` | 从分拣工位画条件线 | 不同问题走不同路线 |
| `this::routeDecision` | 条件判断方法的引用 | 告诉框架"用这个方法来决定走哪条路" |
| `Map.of("local", "localAnswer", ...)` | 条件值到节点的映射 | 如果返回"local"就去localAnswer节点 |
| `.addEdge("localAnswer", END)` | 直接派送到终点 | 简单问题回答完就结束 |

### 3.2 条件路由方法：routeDecision()

```java
private String routeDecision(OverAllState state) {
    // 从全局状态中取出路由级别
    int routeLevel = state.<Integer>value("routeLevel").orElse(2);
    // 从全局状态中取出本地回答
    String localAnswer = state.<String>value("localAnswer").orElse("");

    // 如果是 Level 0 或 1，且有本地回答 → 走"local"分支
    if (routeLevel < 2 && localAnswer != null && !localAnswer.isBlank()) {
        return "local";
    }
    // 否则走"expert"分支
    return "expert";
}
```

**翻译成人话**：
- 如果分拣工位判断这是简单问题（Level 0 问候或 Level 1 常识），并且已经准备好了回答 → 走直接派送
- 否则 → 走专业处理

### 3.3 执行图：execute()

```java
public WorkflowChatResult execute(WorkflowChatRequest request, Long userId) {
    // 1. 准备初始状态（把用户问题、用户ID等打包成包裹信息）
    OverAllState initialState = buildInitialState(request, userId, conversationId);

    // 2. 搭建并编译图（把图纸变成实际的快递站）
    StateGraph graph = buildGraph();
    CompiledGraph compiled = graph.compile();

    // 3. 执行（包裹进入快递站，走完整个流程）
    RunnableConfig config = RunnableConfig.builder().build();
    Optional<OverAllState> finalStateOpt = compiled.invoke(initialState, config);

    // 4. 从最终状态中提取结果
    WorkflowChatResult result = extractResult(finalStateOpt.get());

    // 5. 保存历史记录
    saveHistory(userId, request, result);
    persistExecutionLog(userId, request, result);

    return result;
}
```

---

## 四、GeneralNode 详解 — "分拣工位"

这是整个工作流最核心、最复杂的节点。它做四件事：

### 4.1 功能一：会话向量化（把对话变成数学向量）

**目的**：让系统能"记住"之前的对话，以后可以通过语义搜索找到相关的历史对话。

**代码**：
```java
private void vectorizeLastConversation(Long userId, String conversationId) {
    // 1. 从数据库查最近一条对话
    AiChatHistory lastChat = chatHistoryMapper.selectOne(
        new LambdaQueryWrapper<AiChatHistory>()
            .eq(AiChatHistory::getUserId, userId)           // 条件：这个用户的
            .eq(AiChatHistory::getConversationId, conversationId) // 条件：这个会话的
            .orderByDesc(AiChatHistory::getCreateTime)       // 按时间倒序
            .last("LIMIT 1")                                 // 只取一条
    );

    // 2. 把对话文本切成小块，变成向量，存入向量数据库
    if (lastChat != null) {
        historyVectorizer.vectorizeSync(List.of(lastChat));
    }
}
```

**原理**：
- **向量**就是一串数字，比如 `[0.12, -0.34, 0.56, ...]`
- 两段文字的向量越接近，说明内容越相似
- 用 **Ollama（本地部署的AI模型）** 把文字变成向量
- 存到 **PgVector（PostgreSQL的向量扩展）** 里
- 以后搜索"申论怎么写"，就能找到之前关于申论的对话

**调用链**：
```
GeneralNode.vectorizeLastConversation()
  → AiChatHistoryMapper.selectOne()          // MyBatis-Plus 查数据库
  → ConversationHistoryVectorizer.vectorizeSync()  // 向量化
    → ChunkSplitter.splitText()              // 把长文本切成300字的小块
    → VectorStore.add()                      // 存入PgVector向量数据库
```

### 4.2 功能二：上下文注入（收集用户的所有信息）

**目的**：让 AI 知道"你是谁"、"你之前学了什么"、"你喜欢什么"，这样回答更个性化。

**四个信息来源**：

#### (1) 用户画像
```java
String compressedProfile = profileCompressor.getCachedProfile(userId);
```
- **做了什么**：获取用户的学习偏好描述（500字以内）
- **原理**：`UserProfileCompressor` 用 LLM 把用户的完整画像压缩成简短描述
- **比喻**：就像老师手里的学生档案，写着"小明喜欢考公，擅长行测，申论薄弱"

#### (2) 长期记忆
```java
String memoryContext = getMemoryContext(userId, query);
// 内部调用：
List<UserMemoryEntry> memories = userMemoryService.searchMemories(userId, query, 5);
```
- **做了什么**：搜索与当前问题相关的记忆条目（最多5条）
- **原理**：`UserMemoryService` 先用向量搜索找语义相关的记忆，找不到就查数据库
- **记忆类型**：preference（偏好）、goal（目标）、knowledge（知识）、mistake（错误）、habit（习惯）
- **比喻**：就像老师记得"小明上次申论跑题了"、"小明想考国考"

#### (3) 历史对话向量
```java
List<Document> historyChunks = searchHistoryChunks(userId, query);
// 内部调用：
SearchRequest request = SearchRequest.builder()
    .query(query)                    // 用当前问题作为搜索词
    .topK(3)                         // 最多返回3条
    .similarityThreshold(0.2)        // 相似度阈值
    .filterExpression("source == 'conversation_history' AND user_id == " + userId)
    .build();
List<Document> docs = vectorStore.similaritySearch(request);
```
- **做了什么**：从向量数据库中搜索与当前问题最相似的历史对话片段
- **原理**：把问题变成向量，然后在向量数据库中找最接近的3条
- **比喻**：翻之前的聊天记录，找到最相关的几条

#### (4) 近期对话上下文
```java
String recentContext = getRecentContext(conversationId);
// 内部调用：
List<Message> messages = boundedChatMemory.get(conversationId, 10);
```
- **做了什么**：获取当前会话最近10条消息
- **原理**：`BoundedChatMemory` 是一个有界的内存缓存，最多存1000个会话
- **比喻**：就像看当前聊天窗口最近的几条消息，知道上下文

### 4.3 功能三：路由决策（判断问题复杂度）

系统把问题分成三个级别：

#### Level 0：关键词直接回答（不调用AI）

```java
private String tryKeywordMatch(String query) {
    // 遍历预定义的问候词
    for (Map.Entry<String, String> entry : GREETING_RESPONSES.entrySet()) {
        if (lowerQuery.equals(entry.getKey()) || lowerQuery.startsWith(entry.getKey())) {
            return entry.getValue();  // 直接返回预设回答
        }
    }
    return null;
}
```

**原理**：用 `Map` 存储问候词和预设回答，纯字符串匹配，不需要调用 AI。

**例子**：
| 用户输入 | 匹配到 | 回答 |
|----------|--------|------|
| "你好" | "你好" | "你好！我是你的 AI 学习助手..." |
| "谢谢你" | "谢谢" | "不客气！有问题随时问我哦~" |
| "什么是申论" | 没匹配到 | null（进入下一级判断） |

#### Level 1：简单问题（调用一次AI）

```java
private boolean isSimpleQuestion(String query) {
    if (query.length() > 50) return false;  // 太长不算简单
    return SIMPLE_PATTERNS.stream().anyMatch(p -> p.matcher(query).find());
}
```

**原理**：用正则表达式匹配简单问题模式：
- `^(什么是|介绍一下|解释一下|定义)\s*\S{1,15}$` — 匹配"什么是X"
- `^(\S{1,10}(是什么|是啥|啥意思|什么意思))$` — 匹配"X是什么"
- `^(翻译|translate)\s*` — 匹配翻译请求

**例子**：
| 用户输入 | 长度 | 模式匹配 | 结果 |
|----------|------|----------|------|
| "什么是申论" | 5 | ✅ `什么是` + `申论` | Level 1 |
| "概率论是什么" | 6 | ✅ `是什么` | Level 1 |
| "帮我写一篇申论" | 7 | ❌ 不匹配任何模式 | Level 2 |

#### Level 2：专家路由（需要深度分析）

如果既不是问候，也不是简单问题，就进入专家路由：

```java
private RouteDecision routeToExpert(String query, String materialSummary, String compressedProfile) {
    // 第一步：关键词快速路由
    String fastDomain = detectDomainByKeywords(query);
    if (fastDomain != null) {
        // 包含"申论"→ CIVIL_EXPERT, 包含"考研"→ GRADUATE_EXPERT
        return new RouteDecision(fastDomain, expertId, 0.90, "关键词命中");
    }

    // 第二步：LLM 智能路由
    return analyzeWithLlm(query, compressedProfile, materialSummary);
}
```

**关键词路由**：
```java
"CIVIL", List.of("申论", "行测", "国考", "省考", "面试", "时政", "公务", "公考")
"GRADUATE", List.of("考研", "高数", "概率论", "线性代数", "英一", "英二", "复试")
```
如果问题包含"申论"，直接路由到考公专家，置信度 0.90。

**LLM 路由**（关键词没命中时）：
```java
String prompt = """
    请分析以下用户问题属于哪个学习领域。
    可选领域：
    - CIVIL：公务员考试
    - GRADUATE：研究生考试
    - GENERAL：通用/无法明确归类

    用户问题：%s
    请返回 JSON：{"domain":"GENERAL","intent":"问题意图","confidence":0.5}
    """;
String response = client.prompt().user(prompt).call().content();
```
让 AI 分析问题属于哪个领域，返回 JSON 格式的结果。

### 4.4 功能四：简单问题回答（Level 0/1 生成 localAnswer）

对于 Level 1 的简单问题，GeneralNode 会直接调用 LLM 生成简短回答：

```java
private String generateSimpleAnswer(String query, String compressedProfile, String memoryContext) {
    ChatClient client = agentClientFactory.getOrCreate("general");  // 获取通用Agent的ChatClient
    String prompt = String.format("""
        用户问题：%s
        用户画像：%s
        记忆上下文：%s
        这是一个简单问题，请用 1-3 句话直接回答。
        """, query, compressedProfile, memoryContext);
    return client.prompt().user(prompt).call().content();
}
```

**原理**：
- `agentClientFactory.getOrCreate("general")` — 从 `agent-general.yml` 配置文件创建 ChatClient
- `.prompt().user(prompt).call().content()` — Spring AI 的标准调用链：构建提示词 → 发送给AI → 获取回答

---

## 五、LocalAnswerNode 详解 — "直接派送工位"

这是最简单的节点，只有6行有效代码：

```java
public Map<String, Object> apply(OverAllState state) throws Exception {
    // 从全局状态中取出 GeneralNode 准备好的本地回答
    String localAnswer = state.value("localAnswer", "");
    if (localAnswer == null || localAnswer.isBlank()) {
        localAnswer = "抱歉，我无法理解您的问题，请重新描述。";
    }
    // 写入最终的 answer 字段
    return Map.of("answer", localAnswer);
}
```

**为什么需要这个节点？**
- Graph 架构要求每个分支的终点都是一个节点
- 不能直接从条件边跳到 END
- 这个节点就是 `local` 分支的"终点站"

---

## 六、ExpertAgentNode 详解 — "专业处理工位"

### 6.1 内部路由机制

虽然在图里它是一个节点，但内部会路由到三个不同的专家服务：

```java
public ExpertAgentNode(List<ExpertAgentService> expertServices) {
    // 把所有 ExpertAgentService 实现注册到 Map 中
    // key = expertId, value = service实例
    this.expertServices = expertServices.stream()
        .collect(Collectors.toMap(ExpertAgentService::getExpertId, s -> s));
}

public Map<String, Object> apply(OverAllState state) throws Exception {
    // 从 state 中取出 GeneralNode 决定的专家ID
    String routedExpert = state.value("routedExpert", "GENERAL_EXPERT");

    // 找到对应的专家服务
    ExpertAgentService service = expertServices.get(routedExpert);

    // 调用专家服务生成回答
    String answer = service.answer(state);
    return Map.of("answer", answer);
}
```

**三个专家服务**：

| expertId | Service 类 | 专长 |
|----------|-----------|------|
| CIVIL_EXPERT | CivilExpertAgentService | 考公（申论、行测、面试） |
| GRADUATE_EXPERT | GraduateExpertAgentService | 考研（高数、英语、政治） |
| GENERAL_EXPERT | GeneralQaAgentService | 通用学习助手 |

### 6.2 专家服务的继承结构

```
ExpertAgentService (接口)
    ↑ implements
AbstractExpertAgentService (抽象基类，核心逻辑在这里)
    ↑ extends
    ├── CivilExpertAgentService (只需提供 agentId/expertId/displayName)
    ├── GraduateExpertAgentService
    └── GeneralQaAgentService
```

三个子类只有一行代码，所有逻辑都在基类里。

---

## 七、AbstractExpertAgentService 详解 — "专家工作手册"

### 7.1 核心方法：answer()

```java
public String answer(OverAllState state) {
    // 1. 获取 ChatClient（从 agent-civil.yml 等配置文件创建）
    ChatClient client = agentClientFactory.getOrCreate(agentId);

    // 2. 构建提示词（把所有上下文信息组装成一段话）
    String prompt = buildPrompt(state);

    // 3. 调用 AI，同时传入工具（Spring AI 自动处理 ReAct 循环）
    String answer = client.prompt()
        .user(prompt)           // 用户提示词
        .tools(knowledgeTools)  // 传入工具，AI可以自主调用
        .call()                 // 调用 AI
        .content();             // 获取回答文本

    return answer;
}
```

### 7.2 ReAct 循环（AI 自主调用工具）

**什么是 ReAct？**

ReAct = Reasoning + Acting（推理 + 行动）

AI 不是只回答一次就结束，而是会：
1. **思考**：我需要什么信息来回答这个问题？
2. **行动**：调用工具获取信息
3. **观察**：看看工具返回了什么
4. **再思考**：信息够了吗？不够再调用
5. **回答**：信息足够后生成最终回答

**这个过程是 Spring AI 框架自动处理的**，开发者只需要：
- 定义工具（`@Tool` 注解的方法）
- 把工具传给 ChatClient（`.tools(knowledgeTools)`）

### 7.3 buildPrompt() — 组装提示词

```java
private String buildPrompt(OverAllState state) {
    StringBuilder sb = new StringBuilder();

    // 角色设定
    sb.append("你是").append(displayName).append("。\n\n");

    // 回答要求
    sb.append("## 回答要求\n");
    sb.append("- 优先使用 KnowledgeTools 按需检索\n");
    sb.append("- 不要编造信息\n\n");

    // GeneralNode 注入的上下文
    sb.append("## 用户问题\n").append(state.value("query", ""));
    sb.append("## 用户画像\n").append(state.value("compressedProfile", ""));
    sb.append("## 长期记忆\n").append(state.value("memoryContext", ""));
    sb.append("## 资料分析\n").append(state.value("materialSummary", ""));
    // ... 历史对话、近期对话 ...

    // 工具使用指引
    sb.append("## 可用工具\n");
    sb.append("- searchDomainKnowledge(domain, query)\n");
    sb.append("- searchPersonalMaterial(userId, query)\n");
    sb.append("- searchConversationHistory(userId, query)\n");

    return sb.toString();
}
```

**这样做的好处**：
- AI 拿到的不只是一句话的问题，而是包含用户画像、记忆、历史对话的完整上下文
- AI 知道有哪些工具可以用，需要更多信息时会自己调用

---

## 八、KnowledgeTools 详解 — "AI 的工具箱"

### 8.1 三个 @Tool 工具

```java
@Component
public class KnowledgeTools {

    @Tool(description = "Search the built-in exam knowledge bank by domain")
    public String searchDomainKnowledge(String domain, String query) {
        // 在系统知识库中搜索（source == 'system_knowledge_bank'）
        // 用向量搜索，返回最相关的5条
    }

    @Tool(description = "Search the current user's uploaded study materials")
    public String searchPersonalMaterial(Long userId, String query, String quizType) {
        // 在用户上传的资料中搜索
        // 用混合搜索（向量 + BM25关键词），返回最相关的5条
    }

    @Tool(description = "Search the current user's conversation history")
    public String searchConversationHistory(Long userId, String query) {
        // 在用户的历史对话中搜索
        // 用向量搜索，返回最相关的5条
    }
}
```

### 8.2 @Tool 的工作原理

1. **开发者**在方法上加 `@Tool` 注解，描述工具的用途
2. **Spring AI 框架**自动把工具信息告诉 AI（包括参数说明）
3. **AI** 在需要时决定调用哪个工具、传什么参数
4. **框架**自动执行对应的 Java 方法，把结果返回给 AI
5. **AI** 根据工具返回的结果继续推理或生成最终回答

### 8.3 搜索原理

**向量搜索**：
```
"什么是申论" → [0.12, -0.34, 0.56, ...]  ← 问题变成向量
                              ↓ 比较相似度
知识库中的文档向量 → [0.11, -0.33, 0.55, ...]  ← 很接近！
                              ↓
返回最相似的5条文档
```

**混合搜索（HybridSearchService）**：
- 向量搜索：找语义相似的（"申论"能找到"公务员写作"）
- BM25 关键词搜索：找关键词匹配的（精确匹配"申论"）
- RRF 融合：把两种搜索结果合并排序

---

## 九、数据流全景图

```
用户输入："什么是申论？"
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│ AiController.workflowAsk()                                  │
│   → WorkflowGraphService.execute()                          │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ WorkflowGraphService.buildInitialState()                    │
│   把 question, userId, materialId 等打包成 OverAllState      │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ GeneralNode.apply(state)                                    │
│                                                             │
│ Step 1: 会话向量化                                          │
│   vectorizeLastConversation()                               │
│     → chatHistoryMapper.selectOne()     ← 查数据库最后一条  │
│     → historyVectorizer.vectorizeSync() ← 切片+向量化+存储  │
│                                                             │
│ Step 2: 上下文注入                                          │
│   compressedProfile ← profileCompressor.getCachedProfile()  │
│   memoryContext     ← userMemoryService.searchMemories()    │
│   historyChunks     ← vectorStore.similaritySearch()        │
│   recentContext     ← boundedChatMemory.get()               │
│                                                             │
│ Step 3: 路由决策                                            │
│   tryKeywordMatch("什么是申论") → null（没匹配到问候词）     │
│   isSimpleQuestion("什么是申论") → true（匹配"什么是X"模式） │
│   → Level 1                                                 │
│                                                             │
│ Step 4: 生成简单回答                                        │
│   generateSimpleAnswer()                                    │
│     → agentClientFactory.getOrCreate("general") ← 获取AI    │
│     → client.prompt().user(prompt).call().content() ← 调AI  │
│     → "申论是公务员考试的科目之一..."                        │
│                                                             │
│ 输出到 state:                                               │
│   routeLevel = 1                                            │
│   routedExpert = "GENERAL_EXPERT"                           │
│   localAnswer = "申论是公务员考试的科目之一..."              │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ WorkflowGraphService.routeDecision()                        │
│   routeLevel = 1 (< 2) && localAnswer 不为空                │
│   → return "local"                                          │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ LocalAnswerNode.apply(state)                                │
│   从 state 取出 localAnswer                                  │
│   → Map.of("answer", "申论是公务员考试的科目之一...")        │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ WorkflowGraphService.extractResult()                        │
│   从最终 state 提取 answer, routeLevel 等                    │
│   → WorkflowChatResult                                      │
│                                                             │
│ saveHistory()           ← 保存到 ai_chat_history 表         │
│ persistExecutionLog()   ← 保存到 agent_execution_log 表     │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
                    返回给前端显示
```

---

## 十、如果用户问的是复杂问题呢？

假设用户问："帮我分析一下2024年国考申论的答题技巧"

```
GeneralNode:
  tryKeywordMatch() → null（不是问候）
  isSimpleQuestion() → false（超过50字）
  → Level 2

  routeToExpert():
    detectDomainByKeywords() → "CIVIL"（包含"国考"和"申论"）
    → routedExpert = "CIVIL_EXPERT", confidence = 0.90

  localAnswer = null（Level 2 不生成本地回答）

routeDecision():
  routeLevel = 2 → return "expert"

ExpertAgentNode:
  service = CivilExpertAgentService
  → AbstractExpertAgentService.answer(state)

AbstractExpertAgentService:
  buildPrompt() — 组装包含用户画像、记忆、历史对话的提示词
  client.prompt()
    .user(prompt)           — 发送提示词
    .tools(knowledgeTools)  — 传入3个工具
    .call()                 — 调用AI
    .content()              — 获取回答

  Spring AI ReAct 循环（自动）:
    AI思考: "我需要查一下国考申论的相关知识"
    AI调用: searchDomainKnowledge("CIVIL", "国考申论答题技巧")
    工具返回: [相关知识片段...]
    AI思考: "信息够了，我来生成回答"
    AI回答: "## 2024年国考申论答题技巧\n\n..."
```

---

## 十一、文件清单与职责

| 文件 | 包路径 | 职责 |
|------|--------|------|
| WorkflowGraphService | graph/ | 搭建图、执行图、保存结果 |
| RouteKeys | graph/ | 定义 state 中所有 key 的常量名 |
| WorkflowChatResult | graph/ | 返回给前端的结果 DTO |
| GeneralNode | node/ | 向量化 + 上下文注入 + 路由 + 简单回答 |
| LocalAnswerNode | node/ | 返回 Level 0/1 的本地回答 |
| ExpertAgentNode | node/ | 内部路由到三个专家 Service |
| ExpertAgentService | service/ | 专家服务接口 |
| AbstractExpertAgentService | service/ | 专家服务基类（提示词+工具调用） |
| CivilExpertAgentService | service/ | 考公专家（一行代码） |
| GraduateExpertAgentService | service/ | 考研专家（一行代码） |
| GeneralQaAgentService | service/ | 通用专家（一行代码） |
| KnowledgeTools | agent/tool/ | 3个 @Tool 工具方法 |
| AiController | controller/ | HTTP 接口入口 |
| WorkflowChatRequest | dto/ | 前端传来的请求 DTO |

---

## 十二、关键技术栈

| 技术 | 作用 | 在哪里用到 |
|------|------|-----------|
| Spring AI Alibaba 1.1.2.0 | StateGraph 引擎 | WorkflowGraphService 搭图 |
| Spring AI 1.1.2 | ChatClient + @Tool + VectorStore | 调用AI、定义工具、向量搜索 |
| Ollama + bge-m3 | 本地 Embedding 模型 | 会话向量化 |
| PgVector | PostgreSQL 向量扩展 | 存储和搜索向量 |
| MyBatis-Plus | 数据库 ORM | 查询对话历史、保存记录 |
| Spring Boot 3.x | Web 框架 | 整体架构 |
| Java 21 | 编程语言 | switch 表达式、record 类型 |
