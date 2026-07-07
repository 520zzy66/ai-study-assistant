# Spring AI 框架升级与开发指南

> 版本：Spring AI 1.1.2 + Spring AI Alibaba 1.1.2.0  
> 更新日期：2026-07-01  
> 适用项目：AI Study Assistant

---

## 目录

1. [版本概览](#1-版本概览)
2. [依赖配置](#2-依赖配置)
3. [核心 API 变更](#3-核心-api-变更)
4. [向量存储（PgVectorStore）](#4-向量存储pgvectorstore)
5. [Embedding 模型接入](#5-embedding-模型接入)
6. [Spring AI Alibaba Agent 框架](#6-spring-ai-alibaba-agent-框架)
7. [配置参考](#7-配置参考)
8. [迁移注意事项](#8-迁移注意事项)

---

## 1. 版本概览

### 升级前后对比

| 组件 | 旧版本 | 新版本 | 说明 |
|------|--------|--------|------|
| Spring Boot | 3.3.0 | **3.5.2** | Spring AI Alibaba 要求 3.5.x |
| Spring AI | 1.0.0-M6 | **1.1.2** | 从里程碑版升级到 GA 正式版 |
| Spring AI Alibaba | — | **1.1.2.0** | 新增，提供 Agent 框架 + 向量存储增强 |
| Spring AI Alibaba Extensions | — | **1.1.2.1** | 新增，扩展模块 BOM |

### 新依赖清单

```xml
<!-- Spring AI: pgvector 向量存储 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-pgvector-store</artifactId>
</dependency>

<!-- Spring AI Alibaba: Agent 框架 -->
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-agent-framework</artifactId>
</dependency>

<!-- ONNX Runtime: 本地 Embedding 推理 -->
<dependency>
    <groupId>com.microsoft.onnxruntime</groupId>
    <artifactId>onnxruntime</artifactId>
    <version>1.17.0</version>
</dependency>
```

### BOM 管理（dependencyManagement）

```xml
<dependencyManagement>
    <dependencies>
        <!-- Spring AI BOM -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>1.1.2</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <!-- Spring AI Alibaba BOM -->
        <dependency>
            <groupId>com.alibaba.cloud.ai</groupId>
            <artifactId>spring-ai-alibaba-bom</artifactId>
            <version>1.1.2.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <!-- Spring AI Alibaba Extensions BOM -->
        <dependency>
            <groupId>com.alibaba.cloud.ai</groupId>
            <artifactId>spring-ai-alibaba-extensions-bom</artifactId>
            <version>1.1.2.1</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

---

## 2. 依赖配置

### pom.xml 完整依赖结构

```
spring-boot-starter-parent (3.5.2)
├── spring-boot-starter-web
├── spring-boot-starter-validation
├── spring-boot-starter-actuator
├── spring-boot-starter-data-redis
├── spring-ai-openai-spring-boot-starter    ← ChatClient（MiMo/DeepSeek）
├── spring-ai-pgvector-store                 ← 向量存储（新增）
├── spring-ai-alibaba-agent-framework        ← Agent 框架（新增）
├── onnxruntime (1.17.0)                     ← 本地 Embedding（新增）
├── mybatis-plus-spring-boot3-starter (3.5.7)
├── postgresql
├── jjwt (0.12.6)
├── tika (2.9.1)
├── jieba-analysis (1.0.2)
├── jsoup (1.17.2)
├── hutool-all (5.8.25)
├── springdoc-openapi-starter-webmvc-ui (2.3.0)
├── spring-security-crypto
└── lombok
```

---

## 3. 核心 API 变更

### 3.1 ChatClient（变化不大）

Spring AI 1.1.2 的 `ChatClient` API 与 M6 基本一致，主要变化在内部实现和稳定性：

```java
// 注入方式不变
@Autowired
private ChatClient chatClient;

// 调用方式不变
String response = chatClient.prompt()
    .system("你是一个学习助手")
    .user(question)
    .call()
    .content();

// 流式调用不变
Flux<String> stream = chatClient.prompt()
    .user(question)
    .stream()
    .content();
```

### 3.2 VectorStore（新增核心抽象）

Spring AI 1.1.2 提供了 `VectorStore` 接口，用于向量存储和检索：

```java
public interface VectorStore {
    // 添加文档（自动 Embedding）
    void add(List<Document> documents);
    
    // 删除文档
    void delete(List<String> idList);
    
    // 相似度搜索
    List<Document> similaritySearch(SearchRequest request);
}
```

### 3.3 EmbeddingModel（新增核心抽象）

Spring AI 1.1.2 提供 `EmbeddingModel` 接口，统一 Embedding 调用：

```java
public interface EmbeddingModel {
    // 单条文本 Embedding
    EmbeddingResponse embedForResponse(List<String> texts);
    
    // 快捷方法
    List<double[]> embed(List<String> texts);
    double[] embed(Document document);
    double[] embed(String text);
}
```

### 4. 向量存储（PgVectorStore）

#### 4.1 配置方式

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/ai_study
    username: postgres
    password: ${DB_PASSWORD}
  ai:
    vectorstore:
      pgvector:
        initialize-schema: true          # 首次启动自动建表
        dimensions: 1024                 # 向量维度（bge-m3）
        distance-type: COSINE_DISTANCE   # 余弦相似度（推荐）
        table-name: vector_store          # 向量表名
        index-type: IVFFLAT              # 索引类型：IVFFLAT 或 HNSW
        remove-existing-vector-store-table: false
```

#### 4.2 Java 配置（可选）

如需自定义，可手动声明 Bean：

```java
@Configuration
public class VectorStoreConfig {

    @Bean
    public PgVectorStore vectorStore(
            EmbeddingModel embeddingModel, 
            JdbcTemplate jdbcTemplate) {
        
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(1024)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .removeExistingVectorStoreTable(false)
                .initializeSchema(true)
                .build();
    }
}
```

#### 4.3 使用方式

```java
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;

    /**
     * 添加文档到知识库
     */
    public void addDocuments(Long materialId, List<String> chunks) {
        List<Document> documents = chunks.stream()
            .map(chunk -> new Document(
                chunk,
                Map.of(
                    "material_id", materialId,
                    "chunk_index", chunks.indexOf(chunk),
                    "source", "material_chunk"
                )
            ))
            .toList();
        
        vectorStore.add(documents);
    }

    /**
     * 语义检索
     */
    public List<Document> search(String query, Long materialId, int topK) {
        SearchRequest request = SearchRequest.builder()
            .query(query)
            .topK(topK)
            .similarityThreshold(0.3)
            .filterExpression("material_id == " + materialId)  // 元数据过滤
            .build();
        
        return vectorStore.similaritySearch(request);
    }

    /**
     * 删除资料的所有向量
     */
    public void deleteByMaterialId(Long materialId) {
        SearchRequest request = SearchRequest.builder()
            .query("")  // 空查询，仅靠过滤
            .filterExpression("material_id == " + materialId)
            .build();
        
        // 查询出所有匹配的 ID，然后删除
        List<Document> docs = vectorStore.similaritySearch(request);
        List<String> ids = docs.stream()
            .map(Document::getId)
            .toList();
        
        if (!ids.isEmpty()) {
            vectorStore.delete(ids);
        }
    }
}
```

#### 4.4 距离度量类型

| 类型 | 说明 | 适用场景 |
|------|------|---------|
| `COSINE_DISTANCE` | 余弦距离（1 - cosθ） | **推荐**，与 bge-m3 训练目标一致 |
| `EUCLIDEAN_DISTANCE` | L2 欧氏距离 | 向量已归一化时等价于余弦距离 |
| `NEGATIVE_INNER_PRODUCT` | 负内积 | 向量已归一化时等价于余弦距离 |

#### 4.5 索引类型

| 索引 | 优点 | 缺点 | 适用场景 |
|------|------|------|---------|
| `IVFFLAT` | 构建快、内存小 | 召回率稍低 | < 10 万向量 |
| `HNSW` | 召回率高、查询快 | 构建慢、内存大 | > 10 万向量 |

---

## 5. Embedding 模型接入

#### 5.1 现有接口（保留）

```java
package com.study.ai.rag;

/**
 * Embedding 向量化接口
 * 实现类：OllamaEmbeddingProvider（Ollama + bge-m3，1024 维）
 */
public interface EmbeddingProvider {
    float[] embed(String text);
    List<float[]> embedBatch(List<String> texts);
}
```

#### 5.2 新实现：对接 Spring AI EmbeddingModel

```java
package com.study.ai.rag;

import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring AI EmbeddingModel 适配器
 * 将 EmbeddingProvider 包装为 Spring AI 标准接口
 * 
 * 使用方式：
 * 1. Spring AI PgVectorStore 自动注入此 Bean
 * 2. VectorStore.add() 时自动调用 embed() 转换文本为向量
 */
@Component
public class SpringAiEmbeddingAdapter implements EmbeddingModel {

    private final EmbeddingProvider embeddingProvider;

    public SpringAiEmbeddingAdapter(EmbeddingProvider embeddingProvider) {
        this.embeddingProvider = embeddingProvider;
    }

    @Override
    public EmbeddingResponse embedForResponse(List<String> texts) {
        List<float[]> vectors = embeddingProvider.embedBatch(texts);
        
        List<Embedding> embeddings = vectors.stream()
            .map(v -> new Embedding(v, 0))  // index 会在外层覆盖
            .collect(Collectors.toList());
        
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public List<double[]> embed(List<String> texts) {
        List<float[]> vectors = embeddingProvider.embedBatch(texts);
        return vectors.stream()
            .map(this::floatToDouble)
            .collect(Collectors.toList());
    }

    @Override
    public double[] embed(org.springframework.ai.document.Document document) {
        float[] vector = embeddingProvider.embed(document.getText());
        return floatToDouble(vector);
    }

    private double[] floatToDouble(float[] floats) {
        double[] doubles = new double[floats.length];
        for (int i = 0; i < floats.length; i++) {
            doubles[i] = floats[i];
        }
        return doubles;
    }
}
```

#### 5.3 ONNX 本地推理实现（待实现）

```java
package com.study.ai.rag;

import ai.onnxruntime.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Ollama EmbeddingProvider 实现（当前方案）
 * 模型：bge-m3（1024 维）
 *
 * 使用方式：
 * 1. 本地运行 Ollama 服务（默认 http://localhost:11434）
 * 2. 执行 ollama pull bge-m3 下载模型
 * 3. 配置 spring.ai.ollama.* 即可使用
 */
@Slf4j
@Component
public class OllamaEmbeddingProvider implements EmbeddingProvider {

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${spring.ai.ollama.embedding.model:bge-m3}")
    private String embeddingModel;

    @Value("${ai.embedding.dimension:1024}")
    private int dimension;

    private OllamaEmbeddingModel ollamaEmbeddingModel;

    @PostConstruct
    public void init() {
        OllamaApi ollamaApi = OllamaApi.builder().baseUrl(ollamaBaseUrl).build();
        OllamaEmbeddingOptions options = OllamaEmbeddingOptions.builder().model(embeddingModel).build();
        this.ollamaEmbeddingModel = OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi).defaultOptions(options).build();
    }

    @Override
    public float[] embed(String text) {
        return ollamaEmbeddingModel.embed(text);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        return ollamaEmbeddingModel.embed(texts);
    }
}
```

#### 5.4 模型安装

```bash
# 安装 Ollama 后执行
ollama pull bge-m3
```

---

## 6. Spring AI Alibaba Agent 框架

#### 6.1 核心概念

Spring AI Alibaba 提供了 Agent 抽象，支持多 Agent 协作：

| 概念 | 说明 |
|------|------|
| `Agent` | 智能体接口，包含 `run()` 方法 |
| `AgentTask` | 任务描述，传递给 Agent 执行 |
| `AgentResponse` | Agent 执行结果 |
| `Handoff` | Agent 间任务转交机制 |
| `Toolkit` | 工具集，Agent 可调用的函数 |

#### 6.2 基础使用

```java
// 注入 Agent 框架
@Autowired
private AgentAgentFactory agentFactory;

// 创建并运行 Agent
Agent agent = agentFactory.create(
    AgentOptions.builder()
        .name("考公助手")
        .description("公务员考试专业助手")
        .systemPrompt("你是考公专家...")
        .build()
);

AgentResponse response = agent.run(new AgentTask("申论怎么写？"));
```

#### 6.3 与本项目多 Agent 协作架构的关系

本项目的多 Agent 协作系统（Spec-15）是**基于 Spring AI Alibaba Agent 框架**构建的：

```
Spec-15 自定义 Agent 架构
    ↓ 基于
Spring AI Alibaba Agent 框架
    ↓ 基于
Spring AI 1.1.2 ChatClient + VectorStore + EmbeddingModel
```

**开发规范**：
- 专家 Agent 实现应基于 Spring AI Alibaba 的 `Agent` 接口
- 通用 Agent（Orchestrator）负责路由和任务分发
- 工具调用使用 Spring AI 的 `@Tool` 注解或 Alibaba 的 `Toolkit` 机制

#### 6.4 工具定义示例

```java
/**
 * 知识库检索工具
 * 专家 Agent 调用此工具检索本地资料
 */
@Component
public class RagSearchTool {

    private final VectorStore vectorStore;

    public RagSearchTool(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Tool(description = "在知识库中检索与问题相关的文档切片")
    public List<String> searchKnowledgeBase(
            @ToolParam(description = "检索查询") String query,
            @ToolParam(description = "资料ID") Long materialId,
            @ToolParam(description = "返回数量") int topK) {
        
        SearchRequest request = SearchRequest.builder()
            .query(query)
            .topK(topK)
            .filterExpression("material_id == " + materialId)
            .build();
        
        return vectorStore.similaritySearch(request).stream()
            .map(Document::getText)
            .toList();
    }
}
```

---

## 7. 配置参考

#### 7.1 application.yml（完整 AI 配置）

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/ai_study?currentSchema=public
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:3002}
      password: ${REDIS_PASSWORD:}

  ai:
    # ChatClient 配置（OpenAI 兼容）
    openai:
      client:
        connect-timeout: ${AI_CONNECT_TIMEOUT:10000}
        read-timeout: ${AI_READ_TIMEOUT:60000}
      api-key: ${AI_API_KEY}
      base-url: https://api.xiaomimimo.com
      chat:
        options:
          model: mimo-v2.5
          temperature: 0.7

    # pgvector 向量存储配置
    vectorstore:
      pgvector:
        initialize-schema: true
        dimensions: 1024
        distance-type: COSINE_DISTANCE
        table-name: vector_store
        index-type: IVFFLAT
        remove-existing-vector-store-table: false

# 自定义 AI 配置
ai:
  provider: xiaomi
  chat:
    model: mimo-v2.5
    temperature: 0.7
  embedding:
    provider: ollama             # ollama = 本地 Ollama
    model: bge-m3
    dimension: 1024
  rag:
    enable-bm25: true
    enable-reranking: false
    vector-top-k: 10
    bm25-top-k: 10
    final-top-k: 5
    similarity-threshold: 0.3
    hybrid-alpha: 0.6            # 向量检索权重（BM25 = 1 - alpha）
```

#### 7.2 向量表管理

向量表由 Spring AI PgVectorStore 自动管理，无需手动建表。配置如下：

```yaml
spring:
  ai:
    vectorstore:
      pgvector:
        initialize-schema: true          # 首次启动自动建表（vector_store）
        dimensions: 1024                 # bge-m3 向量维度
        distance-type: COSINE_DISTANCE
        table-name: vector_store
        index-type: IVFFLAT
```

Embedding 模型：bge-m3（Ollama 本地部署），1024 维。

---

## 8. 迁移注意事项

#### 8.1 从 Spring AI 1.0.0-M6 升级到 1.1.2

| 变更项 | 影响 | 处理方式 |
|--------|------|---------|
| ChatClient API | 基本兼容 | 无需修改 |
| EmbeddingModel | 新增接口 | 需要实现适配器 |
| VectorStore | 新增接口 | 使用 PgVectorStore |
| 配置属性 | 部分变更 | `spring.ai.openai.*` 保留，新增 `spring.ai.vectorstore.*` |

#### 8.2 从 Spring Boot 3.3.0 升级到 3.5.2

| 变更项 | 影响 | 处理方式 |
|--------|------|---------|
| Jakarta EE | 已迁移 | 项目已使用 jakarta 命名空间 |
| 自动配置 | 可能冲突 | 检查自定义 @Configuration 是否被覆盖 |
| 依赖版本 | 整体升级 | Spring AI Alibaba 要求 3.5.x |

#### 8.3 升级检查清单

- [ ] `mvn clean install -U` 下载新依赖
- [ ] 检查编译错误（主要是 import 路径变更）
- [ ] 检查自动配置冲突（`--debug` 启动查看）
- [ ] 验证 ChatClient 是否正常调用
- [ ] 验证 VectorStore 是否自动创建表
- [ ] 验证 EmbeddingModel 是否正确注入
- [ ] 运行现有测试确保通过

#### 8.4 常见问题

**Q: PgVectorStore Bean 未注入？**
- 确认 `spring-ai-pgvector-store` 依赖已添加
- 确认 `spring.datasource.*` 配置正确
- 确认 PostgreSQL 已启用 pgvector 扩展

**Q: Embedding 维度不匹配？**
- 确认 `spring.ai.vectorstore.pgvector.dimensions` 与模型输出维度一致
- bge-m3 固定为 1024 维

**Q: Spring AI Alibaba 与 Spring AI 版本冲突？**
- Spring AI Alibaba 1.1.2.0 内置依赖 Spring AI 1.1.2
- 使用 BOM 管理版本，不要手动指定 Spring AI 模块版本

---

## 附录：关键类速查

| 类/接口 | 包 | 用途 |
|---------|-----|------|
| `ChatClient` | `org.springframework.ai.chat.client` | AI 对话客户端 |
| `VectorStore` | `org.springframework.ai.vectorstore` | 向量存储接口 |
| `PgVectorStore` | `org.springframework.ai.vectorstore.pgvector` | pgvector 实现 |
| `EmbeddingModel` | `org.springframework.ai.embedding` | Embedding 接口 |
| `EmbeddingResponse` | `org.springframework.ai.embedding` | Embedding 响应 |
| `Document` | `org.springframework.ai.document` | 文档对象 |
| `SearchRequest` | `org.springframework.ai.vectorstore` | 检索请求 |
| `Agent` | `com.alibaba.cloud.ai.agent` | Agent 接口（Alibaba） |
| `AgentTask` | `com.alibaba.cloud.ai.agent` | Agent 任务（Alibaba） |
| `AgentResponse` | `com.alibaba.cloud.ai.agent` | Agent 响应（Alibaba） |
| `@Tool` | `org.springframework.ai.tool` | 工具注解 |

---

## 参考链接

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [Spring AI Alibaba 官方文档](https://java2ai.com/docs/)
- [Spring AI Alibaba GitHub](https://github.com/alibaba/spring-ai-alibaba)
- [Spring AI PgVector 示例](https://github.com/spring-projects/spring-ai-examples/tree/main/vector-stores/pgvector)
- [BM25 算法详解](https://en.wikipedia.org/wiki/Okapi_BM25)
- [bge-m3 模型](https://huggingface.co/BAAI/bge-m3)
