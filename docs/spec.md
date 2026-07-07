# AI 智能学习助手 - Spec 开发文档（增强版）

> 本文档基于原 spec 文档重构，融合架构改进建议，采用 Spec 驱动开发模式：先定义规格契约，再实现代码。
> 每个 Spec 包含：接口契约、数据模型、行为约束、错误码、验收标准、测试用例。

---

## 目录

- [通用约定](#通用约定)
- [Spec-00: 基础设施](#spec-00-基础设施)
- [Spec-01: 用户认证模块](#spec-01-用户认证模块)
- [Spec-02: 学习资料模块](#spec-02-学习资料模块)
- [Spec-03: 文档解析与切片](#spec-03-文档解析与切片)
- [Spec-04: AI 文档总结](#spec-04-ai-文档总结)
- [Spec-05: RAG 文档问答](#spec-05-rag-文档问答)
- [Spec-06: AI 自动出题](#spec-06-ai-自动出题)
- [Spec-07: AI 学习计划](#spec-07-ai-学习计划)
- [Spec-08: 历史记录模块](#spec-08-历史记录模块)
- [Spec-09: 异步任务模块](#spec-09-异步任务模块)
- [Spec-10: StateGraph 工作流](#spec-10-stategraph-工作流)
- [附录](#附录)

---

## 通用约定

### 响应格式

所有接口统一返回 `Result<T>`：

```json
// 成功
{"code": 200, "message": "success", "data": ...}

// 失败（业务错误）
{"code": 500, "message": "错误描述", "data": null}

// 参数校验失败
{"code": 400, "message": "字段名: 错误信息", "data": null}

// 未认证
{"code": 401, "message": "未登录或token失效", "data": null}

// 无权限
{"code": 403, "message": "无权访问该资源", "data": null}

// 资源不存在
{"code": 404, "message": "资源不存在", "data": null}

// 限流
{"code": 429, "message": "请求过于频繁，请稍后再试", "data": null}
```

### 错误码规范

| 范围 | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 429 | 限流 |
| 500 | 业务错误 / 服务器错误 |
| 1000-1999 | 用户模块错误 |
| 2000-2999 | 资料模块错误 |
| 3000-3999 | AI 模块错误 |

### 认证方式

- 请求头：`Authorization: Bearer <jwt_token>`
- JWT payload：`{userId, username, iat, exp}`
- 有效期：24 小时
- 刷新策略：剩余 2 小时内调用接口返回新 token（响应头 `X-New-Token`）

### 命名规范

- 类名：PascalCase（`UserService`）
- 方法/变量：camelCase（`getUserById`）
- 常量：UPPER_SNAKE_CASE（`MAX_FILE_SIZE`）
- 表名/字段：snake_case（`user_id`）
- 接口路径：kebab-case（`/api/ai/quiz`）

### 分页约定

请求参数：
```
page: 页码，从 1 开始，默认 1
size: 每页条数，默认 10，最大 100
```

响应格式：
```json
{
  "records": [...],
  "total": 100,
  "current": 1,
  "size": 10,
  "pages": 10
}
```

### 时间格式

- 请求/响应：`yyyy-MM-dd HH:mm:ss`
- 日期：`yyyy-MM-dd`
- 时区：`Asia/Shanghai`

---

## Spec-00: 基础设施

### 00.1 配置分层

**文件结构**

```
src/main/resources/
├── application.yml          # 公共配置
├── application-dev.yml      # 开发环境
├── application-prod.yml     # 生产环境
└── application-test.yml     # 测试环境
```

**公共配置（application.yml）**

```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  application:
    name: ai-study-assistant

server:
  port: 8080
  servlet:
    context-path: /api

# 公共配置项，敏感信息走环境变量
jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000

file:
  upload-dir: ${FILE_UPLOAD_DIR:./uploads}
  allowed-types: pdf,doc,docx,txt
  max-size: 52428800  # 50MB

ai:
  provider: deepseek
  chat:
    model: deepseek-chat
    temperature: 0.7
    timeout: 60000
  embedding:
    provider: ${EMBEDDING_PROVIDER:local}
    model: bge-small-zh
    dimension: 512

spring-ai:
  version: 1.0.0  # 正式版
```

### 00.2 JWT 过滤器

**行为约束**
- 除白名单路径外，所有请求必须携带有效 token
- 白名单：`/auth/register`, `/auth/login`, `/doc.html`, `/swagger-ui/**`
- token 解析失败返回 401
- token 过期返回 401（message 区分过期与无效）

**实现要点**

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) {
        String token = extractToken(request);
        if (token == null) {
            chain.doFilter(request, response);
            return;
        }
        try {
            Claims claims = jwtUtil.parseToken(token);
            Long userId = claims.get("userId", Long.class);
            String username = claims.get("username", String.class);
            SecurityContext.setCurrentUser(new UserContext(userId, username));
        } catch (ExpiredJwtException e) {
            writeError(response, 401, "token已过期，请重新登录");
            return;
        } catch (Exception e) {
            writeError(response, 401, "无效的token");
            return;
        }
        chain.doFilter(request, response);
    }
}
```

### 00.3 全局异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return Result.error(400, msg);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleFileSize(MaxUploadSizeExceededException e) {
        return Result.error(400, "文件大小超过限制（50MB）");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleSystem(Exception e) {
        log.error("系统异常", e);
        return Result.error(500, "系统繁忙，请稍后再试");
    }
}
```

### 00.4 用户上下文工具

```java
public class UserContext {
    private static final ThreadLocal<UserInfo> HOLDER = new ThreadLocal<>();

    public static void setCurrentUser(UserInfo user) { HOLDER.set(user); }
    public static UserInfo getCurrentUser() {
        UserInfo user = HOLDER.get();
        if (user == null) throw new BusinessException(401, "未登录");
        return user;
    }
    public static Long getCurrentUserId() { return getCurrentUser().getUserId(); }
    public static void clear() { HOLDER.remove(); }
}
```

**验收标准**
- [ ] 无 token 访问受保护接口返回 401
- [ ] 过期 token 返回 401 且 message 提示过期
- [ ] 参数校验失败返回 400 且 message 列出所有错误字段
- [ ] 系统异常返回 500 且不暴露堆栈

---

## Spec-01: 用户认证模块

### 1.1 注册接口

**请求**

```
POST /api/auth/register
Content-Type: application/json

{
  "username": "string, 4-20位, 必填, 唯一, 仅字母数字下划线",
  "password": "string, 6-20位, 必填",
  "nickname": "string, 可选, 默认=username, 最多20位",
  "email": "string, 可选, 合法邮箱格式"
}
```

**DTO 定义**

```java
@Data
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^\\w{4,20}$", message = "用户名4-20位，仅字母数字下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码6-20位")
    private String password;

    @Size(max = 20, message = "昵称最多20位")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;
}
```

**响应**

```json
// 成功
{"code": 200, "message": "注册成功", "data": null}

// 用户名已存在
{"code": 1001, "message": "用户名已存在", "data": null}
```

**行为约束**
- 密码使用 BCrypt（strength=10）加密存储
- username 唯一性校验（数据库唯一索引 + Service 层预检）
- nickname 为空时默认等于 username
- 注册成功不自动登录，需调用登录接口

**测试用例**

| 用例 | 输入 | 预期 |
|------|------|------|
| 正常注册 | 合法用户名密码 | 200，DB 有记录，密码已加密 |
| 用户名重复 | 已存在的 username | 1001 |
| 用户名过短 | "ab" | 400，提示用户名格式 |
| 密码过短 | "123" | 400，提示密码长度 |
| 邮箱格式错 | "abc" | 400，提示邮箱格式 |

---

### 1.2 登录接口

**请求**

```
POST /api/auth/login
Content-Type: application/json

{
  "username": "string, 必填",
  "password": "string, 必填"
}
```

**响应**

```json
// 成功
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 86400000,
    "user": {
      "id": 1,
      "username": "test",
      "nickname": "测试用户",
      "email": "test@example.com",
      "avatar": null
    }
  }
}

// 凭据错误
{"code": 1002, "message": "用户名或密码错误", "data": null}
```

**行为约束**
- 错误信息统一为"用户名或密码错误"，不区分用户名不存在与密码错误
- 登录失败次数记录（Redis），连续 5 次失败锁定 15 分钟
- 返回 JWT token，payload 包含 `{userId, username, iat, exp}`

**测试用例**

| 用例 | 输入 | 预期 |
|------|------|------|
| 正常登录 | 正确凭据 | 200，返回 token 和用户信息 |
| 密码错误 | 错误密码 | 1002 |
| 用户不存在 | 不存在的 username | 1002（同一错误码） |
| 连续失败 5 次 | 5 次错误密码 | 第 6 次返回锁定提示 |

---

### 1.3 获取个人信息

**请求**

```
GET /api/user/profile
Authorization: Bearer <token>
```

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "test",
    "nickname": "测试用户",
    "email": "test@example.com",
    "avatar": null,
    "createTime": "2026-06-17 10:00:00"
  }
}
```

**行为约束**
- 从 `UserContext` 获取当前用户 ID，查询数据库
- 不返回 password、deleted 字段

---

### 1.4 修改个人信息

**请求**

```
PUT /api/user/profile
Authorization: Bearer <token>
Content-Type: application/json

{
  "nickname": "string, 可选",
  "email": "string, 可选",
  "avatar": "string, 可选, URL"
}
```

**响应**

```json
{"code": 200, "message": "更新成功", "data": null}
```

---

## Spec-02: 学习资料模块

### 2.1 上传文件

**请求**

```
POST /api/material/upload
Content-Type: multipart/form-data
Authorization: Bearer <token>

file: <binary, 必填>
category: "string, 可选, 最多50字符"
```

**响应**

```json
{
  "code": 200,
  "message": "上传成功",
  "data": {
    "id": 1,
    "originalName": "操作系统笔记.pdf",
    "fileType": "pdf",
    "fileSize": 1024000,
    "category": "计算机",
    "status": "processing",
    "createTime": "2026-06-17 10:00:00"
  }
}
```

**行为约束**
- 允许的文件类型：`.pdf`, `.doc`, `.docx`, `.txt`（通过扩展名 + Magic Number 双重校验）
- 最大文件大小：50MB
- 文件存储路径：`./uploads/{userId}/{yyyyMMdd}/{uuid}.{ext}`
- 文件名 UUID 重命名，原始名存 `original_name`
- 上传后异步处理（解析 → 切片 → embedding），状态置为 `processing`
- 处理完成状态变为 `ready`

**文件类型校验**

```java
private static final Map<String, String> MAGIC_NUMBERS = Map.of(
    "pdf", "%PDF-",
    "doc",  "D0CF11E0",  // OLE2
    "docx", "504B0304",  // ZIP
    "txt",  null          // 无固定 magic number
);

public void validateFileType(MultipartFile file) {
    String ext = getFileExtension(file.getOriginalFilename());
    if (!ALLOWED_TYPES.contains(ext)) {
        throw new BusinessException(2001, "不支持的文件类型");
    }
    String magic = MAGIC_NUMBERS.get(ext);
    if (magic != null && !checkMagicNumber(file, magic)) {
        throw new BusinessException(2002, "文件内容与扩展名不匹配");
    }
}
```

**错误码**

| 错误码 | 含义 |
|--------|------|
| 2001 | 不支持的文件类型 |
| 2002 | 文件内容与扩展名不匹配 |
| 2003 | 文件大小超过限制 |
| 2004 | 文件为空 |

**测试用例**

| 用例 | 输入 | 预期 |
|------|------|------|
| 上传 PDF | 合法 PDF | 200，返回 id，status=processing |
| 上传 TXT | 合法 TXT | 200，返回 id |
| 上传 exe | 伪装成 pdf | 2002 |
| 超大文件 | 60MB | 2003 |
| 空文件 | 0 字节 | 2004 |

---

### 2.2 文件列表

**请求**

```
GET /api/material/list?category=计算机&status=ready&page=1&size=10
Authorization: Bearer <token>
```

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "originalName": "操作系统笔记.pdf",
        "fileType": "pdf",
        "fileSize": 1024000,
        "category": "计算机",
        "summary": null,
        "status": "ready",
        "chunkCount": 15,
        "createTime": "2026-06-17 10:00:00"
      }
    ],
    "total": 50,
    "current": 1,
    "size": 10,
    "pages": 5
  }
}
```

**行为约束**
- 只返回当前用户的资料（`user_id = currentUserId`）
- 支持按 category、status 筛选
- 按 create_time 倒序
- 分页参数：page 默认 1，size 默认 10，最大 100

---

### 2.3 获取文件详情

**请求**

```
GET /api/material/{id}
Authorization: Bearer <token>
```

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "originalName": "操作系统笔记.pdf",
    "fileType": "pdf",
    "fileSize": 1024000,
    "category": "计算机",
    "summary": "## 知识点总结\n...",
    "status": "ready",
    "chunkCount": 15,
    "createTime": "2026-06-17 10:00:00"
  }
}
```

**行为约束**
- 只能查看自己的文件
- 他人文件返回 403

---

### 2.4 删除文件

**请求**

```
DELETE /api/material/{id}
Authorization: Bearer <token>
```

**响应**

```json
{"code": 200, "message": "删除成功", "data": null}
```

**行为约束**
- 逻辑删除（`deleted = 1`）
- 只能删除自己的文件
- 同时逻辑删除关联的 `material_chunks`
- 物理文件保留（可定期清理）

---

## Spec-03: 文档解析与切片

### 3.1 异步处理流程

**触发时机**：文件上传成功后自动触发

**流程**

```
1. 更新 material.status = "parsing"
2. 调用 DocumentParser 解析文件为纯文本
3. 调用 ChunkSplitter 切片
4. 对每个 chunk 调用 Embedding 生成向量
5. 批量插入 material_chunks
6. 更新 material.status = "ready"
7. 异常时 status = "failed"，记录 error_msg
```

### 3.2 文档解析

**支持格式**：PDF / DOC / DOCX / TXT

**实现方案**：Apache Tika 统一解析

```java
@Component
public class TikaDocumentParser implements DocumentParser {

    public String parse(InputStream input, String fileType) {
        Tika tika = new Tika();
        tika.setMaxStringLength(10 * 1024 * 1024);  // 10MB 文本上限
        return tika.parseToString(input);
    }
}
```

**行为约束**
- 解析后文本去除多余空白和换行
- 文本超过 10MB 截断并记录警告
- 解析失败记录错误日志，status 置为 failed

### 3.3 文本切片

**切片策略**

| 参数 | 值 | 说明 |
|------|-----|------|
| chunkSize | 800 字符 | 中文优化 |
| overlap | 100 字符 | 上下文重叠 |
| splitStrategy | 段落优先 | 按段落/标题切分 |

**实现要点**

```java
public List<String> split(String text) {
    // 1. 按段落切分
    List<String> paragraphs = splitByParagraph(text);
    // 2. 合并过短段落，拆分过长段落
    List<String> chunks = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    for (String para : paragraphs) {
        if (current.length() + para.length() > CHUNK_SIZE) {
            if (current.length() > 0) {
                chunks.add(current.toString());
                // 保留 overlap
                current = new StringBuilder(lastNChars(current, OVERLAP));
            }
            // 处理超长段落
            if (para.length() > CHUNK_SIZE) {
                chunks.addAll(splitLongParagraph(para));
            }
        }
        current.append(para);
    }
    if (current.length() > 0) chunks.add(current.toString());
    return chunks;
}
```

### 3.4 Embedding 生成

**Provider 选择**（因 DeepSeek 不提供 embedding）

| Provider | 模型 | 维度 | 说明 |
|----------|------|------|------|
| 本地 | bge-small-zh | 512 | 推荐，零成本 |
| 阿里通义 | text-embedding-v2 | 1536 | 国内可用 |
| OpenAI | text-embedding-3-small | 1536 | 需代理 |

**实现要点**

```java
public interface EmbeddingProvider {
    float[] embed(String text);
    List<float[]> embedBatch(List<String> texts);
}

@Component
@ConditionalOnProperty(name = "ai.embedding.provider", havingValue = "local")
public class LocalBgeEmbedding implements EmbeddingProvider {
    // 加载 ONNX 模型
}
```

**行为约束**
- 批量 embedding（每批 20 条），减少 API 调用
- embedding 结果缓存（相同内容不重复计算）
- 失败重试 3 次，指数退避

---

## Spec-04: AI 文档总结

### 4.1 生成总结

**请求**

```
POST /api/ai/summary/{materialId}
Authorization: Bearer <token>
Content-Type: application/json

{
  "force": false  // 是否强制重新生成（已有摘要时）
}
```

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "materialId": 1,
    "summary": "## 知识点总结\n\n### 核心概念\n1. 进程与线程的区别...\n\n### 重点内容\n- 死锁的四个必要条件...",
    "createTime": "2026-06-17 10:05:00"
  }
}
```

**Prompt 模板**

```
你是一个专业的学习助手。请根据以下学习资料内容，生成简洁易懂的知识点总结。

要求：
1. 提炼核心概念和重点，去除冗余信息
2. 使用 Markdown 格式，层次清晰
3. 按知识点分类，使用 ### 二级标题
4. 每个知识点配 1-2 句简要说明
5. 末尾附"复习建议"小节
6. 总字数控制在 800-1500 字

文档内容：
{document_content}
```

**行为约束**
- 读取 `material_chunks` 拼接内容（或读取 `learning_materials.summary` 若已存在且 force=false）
- 文档过长时（>16K 字符）先取摘要再总结
- 总结结果保存到 `learning_materials.summary`
- 同时保存到 `ai_chat_history`（chat_type=summary）
- material 状态必须为 ready，否则返回错误

**错误码**

| 错误码 | 含义 |
|--------|------|
| 3001 | 资料未就绪（仍在处理） |
| 3002 | 资料处理失败 |
| 3003 | AI 调用失败 |
| 3004 | AI 响应超时 |

**验收标准**
- [ ] 返回 Markdown 格式总结
- [ ] 总结保存到 `learning_materials.summary`
- [ ] 历史记录可查
- [ ] force=false 时已有摘要直接返回
- [ ] force=true 时重新生成

---

## Spec-05: RAG 文档问答

### 5.1 提问（同步）

**请求**

```
POST /api/ai/qa
Authorization: Bearer <token>
Content-Type: application/json

{
  "materialId": 1,
  "question": "什么是死锁？",
  "historyLimit": 3  // 可选，携带历史对话轮数，默认 0
}
```

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "answer": "死锁是指两个或多个进程在执行过程中，因争夺资源而造成的一种互相等待的现象...",
    "sources": [
      {"chunkIndex": 3, "content": "死锁的定义与条件...", "score": 0.92},
      {"chunkIndex": 7, "content": "死锁的预防与避免...", "score": 0.85}
    ],
    "conversationId": "uuid"  // 用于多轮对话
  }
}
```

### 5.2 提问（流式）

**请求**

```
POST /api/ai/qa/stream
Authorization: Bearer <token>
Content-Type: application/json
Accept: text/event-stream

{
  "materialId": 1,
  "question": "什么是死锁？"
}
```

**响应（SSE）**

```
data: {"type":"sources","data":[{"chunkIndex":3,"content":"...","score":0.92}]}

data: {"type":"chunk","data":"死锁是指"}

data: {"type":"chunk","data":"两个或多个进程"}

data: {"type":"chunk","data":"在执行过程中..."}

data: {"type":"done","data":{"conversationId":"uuid"}}
```

**Prompt 模板**

```
你是一个专业的学习助手。根据以下参考资料回答用户问题。

参考资料：
{context_chunks}

用户问题：{question}

回答要求：
1. 优先基于参考资料回答，引用具体段落
2. 如果资料中没有相关内容，明确说明"资料中未找到相关内容"
3. 回答准确、简洁、有条理
4. 使用 Markdown 格式
5. 如有必要，可补充通用知识，但需标注"[补充知识]"
```

**RAG 流程**

```
1. 校验 materialId 属于当前用户且 status=ready
2. 对 question 调用 Embedding 生成向量
3. 从 material_chunks 检索 Top-K（K=5）最相似切片
   - 过滤条件：material_id = {materialId}
   - 排序：余弦相似度降序
   - 阈值：score > 0.7（低于阈值视为无关）
4. 若全部低于阈值，返回"资料中未找到相关内容"
5. 拼接 context_chunks（含 chunk_index 标记）
6. 若 historyLimit > 0，携带历史对话
7. 调用 LLM 生成回答
8. 保存到 ai_chat_history（chat_type=qa）
9. 返回 answer + sources
```

**向量检索实现**

```java
public List<ChunkSearchResult> search(Long materialId, float[] queryVec, int topK) {
    // 从内存索引加载该 material 的所有 chunk 向量
    List<MaterialChunk> chunks = chunkMapper.selectByMaterialId(materialId);
    List<ChunkSearchResult> results = new ArrayList<>();

    for (MaterialChunk chunk : chunks) {
        float[] chunkVec = deserializeEmbedding(chunk.getEmbedding());
        float score = cosineSimilarity(queryVec, chunkVec);
        results.add(new ChunkSearchResult(chunk, score));
    }

    return results.stream()
        .filter(r -> r.getScore() > SIMILARITY_THRESHOLD)
        .sorted(Comparator.comparing(ChunkSearchResult::getScore).reversed())
        .limit(topK)
        .collect(Collectors.toList());
}
```

**验收标准**
- [ ] 回答基于文档内容
- [ ] 返回引用的切片信息及相似度
- [ ] 无关问题回答"资料中未找到相关内容"
- [ ] 流式接口逐字返回
- [ ] 多轮对话上下文连贯

---

### 5.3 RAG 检索增强（混合检索 + Reranking）

**问题**：纯向量检索可能召回不准，特别是关键词匹配场景。

**混合检索方案**

```
1. 向量检索：余弦相似度 Top-K（K=10）
2. 关键词检索：BM25 算法 Top-K（K=10）
3. 合并去重：取并集
4. Reranking：Cross-Encoder 重排序，取 Top-5
```

**实现要点**

```java
public List<ChunkSearchResult> hybridSearch(Long materialId, String question, float[] queryVec, int topK) {
    // 1. 向量检索
    List<ChunkSearchResult> vectorResults = vectorSearch(materialId, queryVec, 10);

    // 2. BM25 关键词检索
    List<ChunkSearchResult> bm25Results = bm25Search(materialId, question, 10);

    // 3. 合并去重（按 chunk_id 去重，保留较高分）
    Map<Long, ChunkSearchResult> merged = mergeResults(vectorResults, bm25Results);

    // 4. Reranking（可选，需要 cross-encoder 模型）
    if (enableReranking) {
        return rerank(question, new ArrayList<>(merged.values()), topK);
    }

    return merged.values().stream()
        .sorted(Comparator.comparing(ChunkSearchResult::getScore).reversed())
        .limit(topK)
        .collect(Collectors.toList());
}
```

**BM25 实现**

```java
public List<ChunkSearchResult> bm25Search(Long materialId, String query, int topK) {
    // 分词
    List<String> terms = tokenize(query);

    // 加载该 material 的所有 chunk
    List<MaterialChunk> chunks = chunkMapper.selectByMaterialId(materialId);

    // 计算 BM25 分数
    List<ChunkSearchResult> results = new ArrayList<>();
    for (MaterialChunk chunk : chunks) {
        double score = bm25Score(terms, chunk.getContent());
        results.add(new ChunkSearchResult(chunk, (float) score));
    }

    return results.stream()
        .sorted(Comparator.comparing(ChunkSearchResult::getScore).reversed())
        .limit(topK)
        .collect(Collectors.toList());
}

private double bm25Score(List<String> terms, String document) {
    // BM25 参数
    double k1 = 1.5;
    double b = 0.75;

    // 分词 + 词频统计
    Map<String, Integer> termFreq = countTermFrequency(document);

    double score = 0;
    for (String term : terms) {
        int tf = termFreq.getOrDefault(term, 0);
        double idf = Math.log((N - df(term) + 0.5) / (df(term) + 0.5));
        score += idf * (tf * (k1 + 1)) / (tf + k1 * (1 - b + b * docLen / avgDocLen));
    }
    return score;
}
```

**Reranking 实现（可选）**

```java
public List<ChunkSearchResult> rerank(String query, List<ChunkSearchResult> candidates, int topK) {
    // 使用 cross-encoder 模型重排序
    // 方案1：本地 ONNX 模型（如 bge-reranker-v2-m3）
    // 方案2：调用 API（如阿里通义 rerank 接口）

    List<ScoredChunk> scored = crossEncoder.score(query, candidates);
    return scored.stream()
        .sorted(Comparator.comparing(ScoredChunk::getScore).reversed())
        .limit(topK)
        .map(s -> new ChunkSearchResult(s.getChunk(), s.getScore()))
        .collect(Collectors.toList());
}
```

**配置项**

```yaml
ai:
  rag:
    enable-bm25: true
    enable-reranking: false  # 需要 cross-encoder 模型，默认关闭
    vector-top-k: 10
    bm25-top-k: 10
    final-top-k: 5
    similarity-threshold: 0.7
```

**依赖补充**

```xml
<!-- 中文分词（BM25 需要） -->
<dependency>
    <groupId>com.huaban</groupId>
    <artifactId>jieba-analysis</artifactId>
    <version>1.0.2</version>
</dependency>

<!-- Cross-Encoder（可选，Reranking 需要） -->
<dependency>
    <groupId>ai.djl</groupId>
    <artifactId>api</artifactId>
    <version>0.26.0</version>
</dependency>
```

**行为约束**
- 混合检索默认开启，BM25 + 向量检索并行执行
- Reranking 默认关闭（需要额外模型），可通过配置开启
- 最终返回 Top-5 结果，相似度阈值 0.7
- 检索结果记录日志，用于后续优化

---

## Spec-06: AI 自动出题

### 6.1 生成练习题

**请求**

```
POST /api/ai/quiz/{materialId}
Authorization: Bearer <token>
Content-Type: application/json

{
  "choiceCount": 5,
  "judgeCount": 3,
  "shortAnswerCount": 2,
  "difficulty": "medium"  // easy/medium/hard，默认 medium
}
```

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "materialId": 1,
    "batchId": "uuid",
    "questions": [
      {
        "id": 101,
        "type": "choice",
        "difficulty": "medium",
        "question": "以下哪个不是操作系统的核心功能？",
        "options": {"A": "进程管理", "B": "内存管理", "C": "文字处理", "D": "文件管理"},
        "answer": "C",
        "explanation": "文字处理属于应用软件功能，不是操作系统核心功能。"
      },
      {
        "id": 102,
        "type": "judge",
        "difficulty": "easy",
        "question": "进程和线程是同一个概念。",
        "answer": "false",
        "explanation": "进程是资源分配的基本单位，线程是CPU调度的基本单位。"
      },
      {
        "id": 103,
        "type": "short_answer",
        "difficulty": "hard",
        "question": "简述死锁的四个必要条件。",
        "answer": "互斥条件、请求与保持条件、不可剥夺条件、循环等待条件。",
        "explanation": "这四个条件是死锁产生的必要条件，破坏任一条件即可预防死锁。"
      }
    ]
  }
}
```

**Prompt 模板**

```
你是一个专业的出题专家。根据以下学习资料生成练习题。

文档内容：
{document_content}

出题要求：
1. 生成 {choiceCount} 道单选题，选项 A/B/C/D
2. 生成 {judgeCount} 道判断题，答案为 true/false
3. 生成 {shortAnswerCount} 道简答题
4. 难度等级：{difficulty}
5. 每题附带正确答案和详细解析
6. 题目必须基于文档内容，不得编造
7. 选项分布均匀（答案不全是 C）

严格返回以下 JSON 格式（不要包含 markdown 代码块标记）：
{
  "questions": [
    {
      "type": "choice",
      "difficulty": "medium",
      "question": "题干",
      "options": {"A": "选项A", "B": "选项B", "C": "选项C", "D": "选项D"},
      "answer": "C",
      "explanation": "解析"
    },
    {
      "type": "judge",
      "difficulty": "easy",
      "question": "题干",
      "answer": "false",
      "explanation": "解析"
    },
    {
      "type": "short_answer",
      "difficulty": "hard",
      "question": "题干",
      "answer": "参考答案",
      "explanation": "解析"
    }
  ]
}
```

**行为约束**
- 读取文档内容（chunk 拼接，超长则截断到 16K）
- 使用 `BeanOutputConverter` 强制 JSON 解析，解析失败重试 1 次
- temperature 设置为 0.3（保证稳定性）
- 结果批量保存到 `ai_question_bank`，同一批次共享 `batch_id`
- 保存到 `ai_chat_history`（chat_type=quiz）

**错误码**

| 错误码 | 含义 |
|--------|------|
| 3001 | 资料未就绪 |
| 3005 | AI 返回格式异常 |
| 3006 | 出题数量超限（单次最多 20 题） |

**验收标准**
- [ ] 返回指定数量和类型的题目
- [ ] 每题有答案和解析
- [ ] 题目内容基于文档
- [ ] JSON 格式严格符合定义
- [ ] 选项答案分布合理

---

### 6.2 用户作答与判分

**请求**

```
POST /api/ai/quiz/{batchId}/answer
Authorization: Bearer <token>
Content-Type: application/json

{
  "answers": [
    {"questionId": 101, "answer": "C"},
    {"questionId": 102, "answer": "true"},
    {"questionId": 103, "answer": "互斥条件、请求与保持条件、不可剥夺条件、循环等待条件"}
  ]
}
```

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "batchId": "uuid",
    "totalCount": 3,
    "correctCount": 2,
    "score": 66.7,
    "details": [
      {
        "questionId": 101,
        "userAnswer": "C",
        "correctAnswer": "C",
        "isCorrect": true,
        "explanation": "文字处理属于应用软件功能，不是操作系统核心功能。"
      },
      {
        "questionId": 102,
        "userAnswer": "true",
        "correctAnswer": "false",
        "isCorrect": false,
        "explanation": "进程是资源分配的基本单位，线程是CPU调度的基本单位。"
      },
      {
        "questionId": 103,
        "userAnswer": "互斥条件、请求与保持条件、不可剥夺条件、循环等待条件",
        "correctAnswer": "互斥条件、请求与保持条件、不可剥夺条件、循环等待条件",
        "isCorrect": true,
        "score": 1.0,
        "explanation": "这四个条件是死锁产生的必要条件，破坏任一条件即可预防死锁。"
      }
    ]
  }
}
```

**行为约束**
- 选择题/判断题：精确匹配自动判分
- 简答题：调用 AI 进行语义判分（0-1 分），Prompt 模板：

```
你是一个严格的阅卷老师。请判断学生的答案是否正确。

题目：{question}
参考答案：{reference_answer}
学生答案：{student_answer}

评分要求：
1. 核心概念一致即可得分，不要求逐字匹配
2. 返回 JSON：{"score": 0.8, "feedback": "缺少循环等待条件的说明"}
```

- 判分结果保存到 `ai_quiz_record` 表
- 错题自动加入错题本

**错误码**

| 错误码 | 含义 |
|--------|------|
| 3007 | 批次不存在 |
| 3008 | 题目不属于该批次 |
| 3009 | 答案格式错误 |

---

### 6.3 错题本

**数据模型**

```sql
CREATE TABLE IF NOT EXISTS user_wrong_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL COMMENT '关联 ai_question_bank.id',
    material_id BIGINT COMMENT '来源资料',
    question_type VARCHAR(20) NOT NULL,
    user_answer TEXT COMMENT '用户答案',
    correct_answer TEXT COMMENT '正确答案',
    wrong_count INT DEFAULT 1 COMMENT '错误次数',
    last_wrong_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_mastered TINYINT DEFAULT 0 COMMENT '是否已掌握 0/1',
    master_time DATETIME COMMENT '掌握时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_question (user_id, question_id),
    INDEX idx_user_id (user_id),
    INDEX idx_material_id (material_id),
    INDEX idx_is_mastered (is_mastered)
) ENGINE=InnoDB COMMENT='用户错题本';
```

**获取错题列表**

```
GET /api/quiz/wrong?materialId=1&isMastered=false&page=1&size=10
Authorization: Bearer <token>
```

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "questionId": 102,
        "questionType": "judge",
        "question": "进程和线程是同一个概念。",
        "userAnswer": "true",
        "correctAnswer": "false",
        "wrongCount": 2,
        "lastWrongTime": "2026-06-17 10:00:00",
        "materialName": "操作系统笔记.pdf"
      }
    ],
    "total": 15,
    "current": 1,
    "size": 10
  }
}
```

**标记已掌握**

```
PUT /api/quiz/wrong/{id}/master
Authorization: Bearer <token>
```

**行为约束**
- 同一题多次答错，`wrong_count` 累加
- 标记已掌握后不再显示（可筛选）
- 支持按 materialId 筛选
- 支持重新练习错题

---

## Spec-07: AI 学习计划

### 7.1 生成学习计划

**请求**

```
POST /api/ai/plan
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "操作系统期末复习",
  "goal": "掌握操作系统核心概念，通过期末考试",
  "examDate": "2026-07-15",
  "dailyHours": 2,
  "materialIds": [1, 2]  // 可选，关联学习资料
}
```

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "title": "操作系统期末复习",
    "goal": "掌握操作系统核心概念，通过期末考试",
    "examDate": "2026-07-15",
    "dailyHours": 2,
    "totalDays": 28,
    "plan": [
      {
        "day": 1,
        "date": "2026-06-17",
        "weekday": "周三",
        "topics": ["进程管理基础", "进程与线程的区别"],
        "duration": "2小时",
        "tasks": "阅读教材第3章，做课后习题1-5",
        "materials": ["操作系统笔记.pdf"]
      },
      {
        "day": 2,
        "date": "2026-06-18",
        "weekday": "周四",
        "topics": ["进程同步", "死锁"],
        "duration": "2小时",
        "tasks": "阅读教材第4章，重点理解信号量机制",
        "materials": []
      }
    ]
  }
}
```

**Prompt 模板**

```
你是一个专业的学习规划师。请根据以下信息制定个性化学习计划。

学习目标：{goal}
考试日期：{examDate}
今天日期：{today}
每天可用时间：{dailyHours} 小时
关联资料：{material_names}

要求：
1. 从今天到考试日期按天规划
2. 合理分配知识点，循序渐进，先基础后综合
3. 每天任务量与可用时间匹配
4. 包含复习日和模拟测试日
5. 最后一周为冲刺复习

严格返回以下 JSON 格式：
{
  "plan": [
    {
      "day": 1,
      "date": "2026-06-17",
      "weekday": "周三",
      "topics": ["主题1", "主题2"],
      "duration": "2小时",
      "tasks": "具体任务描述",
      "materials": ["资料名"]
    }
  ]
}
```

**行为约束**
- 计划保存到 `study_plan`，`plan_content` 存 JSON 字符串
- 保存到 `ai_chat_history`（chat_type=plan）
- 考试日期不能早于今天
- 总天数超过 90 天时提示拆分

**验收标准**
- [ ] 计划覆盖从今天到考试日期
- [ ] 每天有明确的学习任务
- [ ] 计划保存到数据库
- [ ] JSON 格式正确

---

## Spec-08: 历史记录模块

### 8.1 对话历史列表

**请求**

```
GET /api/history/chat?type=qa&page=1&size=10
Authorization: Bearer <token>
```

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "chatType": "qa",
        "materialId": 1,
        "materialName": "操作系统笔记.pdf",
        "userMessage": "什么是死锁？",
        "aiResponse": "死锁是指...",
        "createTime": "2026-06-17 10:00:00"
      }
    ],
    "total": 50,
    "current": 1,
    "size": 10,
    "pages": 5
  }
}
```

**行为约束**
- 按 chatType 筛选：summary / qa / quiz / plan
- 按 create_time 倒序
- 只返回当前用户的记录
- 支持按 materialId 二次筛选

### 8.2 出题记录列表

**请求**

```
GET /api/history/quiz?materialId=1&page=1&size=10
Authorization: Bearer <token>
```

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "batchId": "uuid",
        "materialId": 1,
        "materialName": "操作系统笔记.pdf",
        "questionCount": 10,
        "createTime": "2026-06-17 10:00:00"
      }
    ],
    "total": 20,
    "current": 1,
    "size": 10
  }
}
```

### 8.3 出题详情

**请求**

```
GET /api/history/quiz/{batchId}
Authorization: Bearer <token>
```

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "batchId": "uuid",
    "materialId": 1,
    "questions": [...]
  }
}
```

---

## Spec-09: 异步任务模块

### 9.1 任务状态查询

**请求**

```
GET /api/task/{taskId}
Authorization: Bearer <token>
```

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": "uuid",
    "type": "material_process",
    "status": "running",
    "progress": 60,
    "message": "正在生成 Embedding (9/15)",
    "result": null,
    "createTime": "2026-06-17 10:00:00",
    "updateTime": "2026-06-17 10:01:30"
  }
}
```

**任务状态**

| 状态 | 含义 |
|------|------|
| pending | 排队中 |
| running | 执行中 |
| success | 成功 |
| failed | 失败 |

### 9.2 任务列表

**请求**

```
GET /api/task?type=material_process&status=running&page=1&size=10
Authorization: Bearer <token>
```

**行为约束**
- 只返回当前用户的任务
- 支持按 type、status 筛选

### 9.3 数据模型

**新增表：ai_task**

```sql
CREATE TABLE IF NOT EXISTS ai_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id VARCHAR(64) NOT NULL UNIQUE COMMENT '任务UUID',
    user_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL COMMENT 'material_process/summary/quiz/plan',
    ref_id BIGINT COMMENT '关联资源ID（如 materialId）',
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    progress INT DEFAULT 0 COMMENT '进度 0-100',
    message VARCHAR(500),
    result JSON COMMENT '任务结果',
    error_msg TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_task_id (task_id),
    INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='AI异步任务表';
```

---

## Spec-10: StateGraph 工作流

> **架构**：Spring AI Alibaba StateGraph 状态图引擎
> **详细设计**：`docs/workflow-orchestrator-spec.md`
> **实现说明**：`docs/workflow-implementation-guide.md`

### 10.1 整体架构

AI 对话功能封装为 StateGraph 状态图工作流：

```
START → GeneralNode → (localAnswer | expertAgent) → END
```

- **GeneralNode**：路由决策 + 上下文注入（画像/记忆/历史/资料）+ 会话向量化 + 简单问题回答
- **LocalAnswerNode**：返回 Level 0/1 的本地回答
- **ExpertAgentNode**：内部路由到三个专家 Agent Service（Civil/Graduate/General）

### 10.2 接口

**同步问答**

```
POST /ai/workflow/ask
Authorization: Bearer <token>
Content-Type: application/json

{
  "question": "什么是申论？",
  "materialId": null,
  "conversationId": "xxx",
  "history": []
}
```

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "answer": "申论是公务员考试科目之一...",
    "routeLevel": 1,
    "routedExpert": "GENERAL_EXPERT",
    "routeDomain": "GENERAL",
    "routeIntent": "简单常识问题",
    "routeConfidence": 0.85,
    "conversationId": "xxx",
    "durationMs": 1200,
    "fallback": false
  }
}
```

**流式问答（SSE）**

```
POST /ai/workflow/ask/stream
Authorization: Bearer <token>
Content-Type: application/json
Accept: text/event-stream

// 请求体同上
```

**SSE 事件格式**

```
event: done
data: {"type":"done","answer":"...","level":1,"agent":"GENERAL_EXPERT","duration_ms":1200,"conversationId":"xxx"}
```

### 10.3 三级路由

| Level | 场景 | 处理方式 | 示例 |
|-------|------|----------|------|
| 0 | 问候/关键词 | 预设回答，不调用 LLM | "你好"、"谢谢" |
| 1 | 简单常识 | 调用一次 LLM，简短回答 | "什么是申论" |
| 2 | 专家分析 | 路由到 ExpertAgent，ReAct 循环 | "帮我分析国考申论技巧" |

### 10.4 上下文注入

GeneralNode 在路由前必须注入以下上下文：

| 上下文 | 来源 | 作用 |
|--------|------|------|
| 用户画像 | `UserProfileCompressor.getCachedProfile()` | 了解用户学习偏好 |
| 长期记忆 | `UserMemoryService.searchMemories()` | 检索相关记忆条目 |
| 历史对话向量 | `VectorStore.similaritySearch()` | 语义检索历史对话 |
| 近期对话 | `BoundedChatMemory.get()` | 最近 10 条消息 |
| 资料分析 | state 中 MultimodalNode 输出 | 上传资料的摘要 |

### 10.5 专家 Agent

| expertId | Service | 专长 |
|----------|---------|------|
| CIVIL_EXPERT | CivilExpertAgentService | 考公（申论、行测、面试） |
| GRADUATE_EXPERT | GraduateExpertAgentService | 考研（高数、英语、政治） |
| GENERAL_EXPERT | GeneralQaAgentService | 通用学习助手 |

每个专家通过 `@Tool` 注解暴露 3 个工具，Spring AI 自动 ReAct 循环：

- `searchDomainKnowledge(domain, query)` — 检索系统知识库
- `searchPersonalMaterial(userId, query)` — 检索用户资料
- `searchConversationHistory(userId, query)` — 检索历史对话

### 10.6 会话向量化

GeneralNode 在每次请求时将上一轮对话向量化存入 PgVector：

- 调用 `ConversationHistoryVectorizer.vectorizeSync()`
- 使用本地 Ollama bge-m3 Embedding 模型
- 切片 300 字/片，50 字重叠
- 元数据包含 `source=conversation_history`, `user_id`

### 10.7 错误码

| 错误码 | 含义 | 触发条件 |
|--------|------|----------|
| 7001 | Agent 不存在 | routedExpert 对应的 Service 未找到 |
| 7009 | 本地模型调用失败 | Level 0/1 LLM 异常 |
| 7010 | 工具调用失败 | @Tool 方法异常 |

### 10.8 数据模型

**执行日志表：agent_execution_log**

```sql
CREATE TABLE IF NOT EXISTS agent_execution_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    conversation_id VARCHAR(64),
    question TEXT,
    route_level INT DEFAULT 2,
    routed_expert VARCHAR(50),
    route_domain VARCHAR(20),
    route_intent VARCHAR(200),
    route_confidence DOUBLE DEFAULT 0.0,
    routing JSON,
    execution_chain JSON,
    final_answer TEXT,
    fallback INT DEFAULT 0,
    error_message VARCHAR(500),
    total_duration_ms BIGINT DEFAULT 0,
    llm_call_count INT DEFAULT 0,
    tool_call_count INT DEFAULT 0,
    total_tokens INT DEFAULT 0,
    user_feedback VARCHAR(10),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_conversation_id (conversation_id)
) ENGINE=InnoDB COMMENT='Agent执行日志';
```

### 10.9 文件结构

```
backend/src/main/java/com/study/ai/
├── workflow/
│   ├── graph/
│   │   ├── RouteKeys.java              # State key 常量
│   │   ├── WorkflowGraphService.java   # StateGraph 构建与执行
│   │   └── WorkflowChatResult.java     # 结果 DTO
│   ├── node/
│   │   ├── GeneralNode.java            # 路由+上下文+向量化+回答
│   │   ├── LocalAnswerNode.java        # Level 0/1 本地回答
│   │   ├── ExpertAgentNode.java        # 专家 Agent 节点
│   │   ├── MultimodalNode.java         # 资料预处理
│   │   └── NodeType.java               # 节点类型枚举
│   └── service/
│       ├── ExpertAgentService.java     # 专家服务接口
│       ├── AbstractExpertAgentService.java # 专家服务基类
│       ├── CivilExpertAgentService.java    # 考公专家
│       ├── GraduateExpertAgentService.java # 考研专家
│       └── GeneralQaAgentService.java      # 通用专家
├── agent/tool/
│   ├── KnowledgeTools.java             # @Tool 方法（RAG 检索）
│   └── ToolCallEventPublisher.java     # 工具调用事件发布
├── profile/
│   └── UserProfileCompressor.java      # 用户画像压缩
├── history/
│   └── ConversationHistoryVectorizer.java # 会话向量化
├── memory/
│   ├── UserMemoryExtractor.java        # 记忆提取
│   └── BoundedChatMemory.java          # 有界对话记忆
└── rag/
    ├── HybridSearchService.java        # 混合检索
    ├── Bm25Service.java                # BM25 检索
    └── OllamaEmbeddingProvider.java    # Ollama Embedding
```

### 10.10 验收标准

- [x] StateGraph 正确构建（节点 + 条件边）
- [x] 条件路由根据 routeLevel 正确分流
- [x] 节点通过 OverAllState 共享数据
- [x] 会话向量化正常工作（本地 Ollama）
- [x] 用户画像/记忆/历史/资料注入到 state
- [x] Level 0 关键词直接回答
- [x] Level 1 简单问题 LLM 回答
- [x] Level 2 专家路由（关键词 + LLM）
- [x] 专家 Agent 自主调用 @Tool
- [x] 路由失败回退 GENERAL_EXPERT
- [x] 专家执行失败返回友好提示
- [x] WorkflowChatResult 字段向后兼容
- [x] agent_execution_log 表结构不变

---

## 附录

### A.1 完整文件结构

```
backend/
├── pom.xml
└── src/main/
    ├── java/com/study/
    │   ├── AiStudyApplication.java
    │   ├── common/
    │   │   ├── Result.java
    │   │   ├── BusinessException.java
    │   │   ├── GlobalExceptionHandler.java
    │   │   ├── UserContext.java
    │   │   └── Constants.java
    │   ├── config/
    │   │   ├── WebConfig.java
    │   │   ├── MyBatisPlusConfig.java
    │   │   ├── JwtConfig.java
    │   │   ├── AiConfig.java
    │   │   ├── AsyncConfig.java
    │   │   └── SwaggerConfig.java
    │   ├── security/
    │   │   ├── JwtUtil.java
    │   │   ├── JwtAuthenticationFilter.java
    │   │   └── SecurityContext.java
    │   ├── entity/
    │   │   ├── User.java
    │   │   ├── LearningMaterial.java
    │   │   ├── MaterialChunk.java
    │   │   ├── AiChatHistory.java
    │   │   ├── StudyPlan.java
    │   │   ├── AiQuestionBank.java
    │   │   ├── AiTask.java
    │   │   ├── AiQuizRecord.java
    │   │   └── UserWrongQuestion.java
    │   ├── mapper/
    │   │   └── ...Mapper.java
    │   ├── dto/
    │   │   ├── request/
    │   │   └── response/
    │   ├── vo/
    │   │   ├── UserVO.java
    │   │   ├── MaterialVO.java
    │   │   └── ...
    │   ├── service/
    │   │   ├── UserService.java
    │   │   ├── AuthService.java
    │   │   ├── MaterialService.java
    │   │   ├── HistoryService.java
    │   │   ├── TaskService.java
    │   │   └── impl/
    │   ├── controller/
    │   │   ├── AuthController.java
    │   │   ├── UserController.java
    │   │   ├── MaterialController.java
    │   │   ├── AiController.java
    │   │   ├── HistoryController.java
    │   │   └── TaskController.java
    │   └── ai/
    │       ├── client/
    │       │   ├── AiClient.java
    │       │   └── EmbeddingProvider.java
    │       ├── agent/
    │       │   ├── config/
    │       │   │   └── AgentClientFactory.java
    │       │   └── tool/
    │       │       ├── KnowledgeTools.java
    │       │       └── ToolCallEventPublisher.java
    │       ├── workflow/
    │       │   ├── graph/
    │       │   │   ├── RouteKeys.java
    │       │   │   ├── WorkflowGraphService.java
    │       │   │   └── WorkflowChatResult.java
    │       │   ├── node/
    │       │   │   ├── GeneralNode.java
    │       │   │   ├── LocalAnswerNode.java
    │       │   │   ├── ExpertAgentNode.java
    │       │   │   └── MultimodalNode.java
    │       │   └── service/
    │       │       ├── ExpertAgentService.java
    │       │       ├── AbstractExpertAgentService.java
    │       │       ├── CivilExpertAgentService.java
    │       │       ├── GraduateExpertAgentService.java
    │       │       └── GeneralQaAgentService.java
    │       ├── profile/
    │       │   └── UserProfileCompressor.java
    │       ├── history/
    │       │   └── ConversationHistoryVectorizer.java
    │       ├── memory/
    │       │   ├── UserMemoryExtractor.java
    │       │   └── BoundedChatMemory.java
    │       ├── rag/
    │       │   ├── RagService.java
    │       │   ├── HybridSearchService.java
    │       │   ├── Bm25Service.java
    │       │   └── OllamaEmbeddingProvider.java
    │       ├── prompt/
    │       │   └── PromptTemplates.java
    │       ├── parser/
    │       │   ├── DocumentParser.java
    │       │   └── TikaDocumentParser.java
    │       ├── chunk/
    │       │   └── ChunkSplitter.java
    │       └── service/
    │           ├── AiSummaryService.java
    │           ├── AiQaService.java
    │           ├── AiQuizService.java
    │           ├── AiPlanService.java
    │           └── QuizGradingService.java
    └── resources/
        ├── application.yml
        ├── application-dev.yml
        ├── application-prod.yml
        └── mapper/
            └── *.xml

sql/
├── schema.sql
└── data.sql

frontend/
├── package.json
├── vite.config.js
└── src/
    ├── main.js
    ├── App.vue
    ├── router/
    ├── stores/
    ├── api/
    ├── views/
    └── components/
```

### A.2 补充依赖

```xml
<!-- Spring AI 正式版 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- API 文档 -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>

<!-- 文档解析 -->
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-core</artifactId>
    <version>2.9.1</version>
</dependency>
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-parsers-standard-package</artifactId>
    <version>2.9.1</version>
</dependency>

<!-- 工具库 -->
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>5.8.25</version>
</dependency>

<!-- BCrypt -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>

<!-- 监控 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- 中文分词（BM25 混合检索需要） -->
<dependency>
    <groupId>com.huaban</groupId>
    <artifactId>jieba-analysis</artifactId>
    <version>1.0.2</version>
</dependency>
```

### A.3 数据库索引补充

```sql
-- 补充联合索引
ALTER TABLE material_chunks ADD INDEX idx_user_material (user_id, material_id);
ALTER TABLE ai_chat_history ADD INDEX idx_user_type_time (user_id, chat_type, create_time);
ALTER TABLE ai_question_bank ADD INDEX idx_user_type (user_id, question_type);
ALTER TABLE ai_question_bank ADD INDEX idx_batch_id (batch_id);

-- 错题本相关索引
ALTER TABLE user_wrong_question ADD INDEX idx_user_material (user_id, material_id);
ALTER TABLE user_wrong_question ADD INDEX idx_user_mastered (user_id, is_mastered);

-- 出题记录表
CREATE TABLE IF NOT EXISTS ai_quiz_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    batch_id VARCHAR(64) NOT NULL,
    question_id BIGINT NOT NULL,
    user_answer TEXT,
    is_correct TINYINT DEFAULT 0,
    score DECIMAL(3,2) DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_batch (user_id, batch_id),
    INDEX idx_batch_id (batch_id)
) ENGINE=InnoDB COMMENT='出题作答记录';
```

### A.4 接口清单汇总

| 模块 | 接口 | 方法 | 认证 |
|------|------|------|------|
| 认证 | /auth/register | POST | 否 |
| 认证 | /auth/login | POST | 否 |
| 用户 | /user/profile | GET | 是 |
| 用户 | /user/profile | PUT | 是 |
| 资料 | /material/upload | POST | 是 |
| 资料 | /material/list | GET | 是 |
| 资料 | /material/{id} | GET | 是 |
| 资料 | /material/{id} | DELETE | 是 |
| AI | /ai/summary/{id} | POST | 是 |
| AI | /ai/qa | POST | 是 |
| AI | /ai/qa/stream | POST | 是 |
| AI | /ai/workflow/ask | POST | 是 |
| AI | /ai/workflow/ask/stream | POST | 是 |
| AI | /ai/quiz/{id} | POST | 是 |
| AI | /ai/quiz/{batchId}/answer | POST | 是 |
| AI | /ai/plan | POST | 是 |
| 出题 | /quiz/wrong | GET | 是 |
| 出题 | /quiz/wrong/{id}/master | PUT | 是 |
| 历史 | /history/chat | GET | 是 |
| 历史 | /history/quiz | GET | 是 |
| 历史 | /history/quiz/{batchId} | GET | 是 |
| 任务 | /task/{id} | GET | 是 |
| 任务 | /task | GET | 是 |

### A.5 开发顺序（推荐）

```
Phase 0: 基础设施（配置分层 + JWT + 异常处理 + Swagger）
    ↓
Phase 1: 用户模块（注册 + 登录 + Profile）
    ↓
Phase 2: 文件上传 + 文档解析（Tika + 切片 + Embedding）
    ↓
Phase 3: AI 基础（AiClient 封装 + Prompt 模板 + 异步任务）
    ↓
Phase 4: AI 业务（总结 → 出题 → 计划 → RAG 问答）
    ↓
Phase 5: 前端工程（Vue3 + 登录 + 资料管理 + AI 功能页）
    ↓
Phase 6: 优化（向量库升级 + 流式响应 + 测试 + 部署）
```
