# 讯飞多模态资源生成接入开发 Spec

> 适用范围：资源工坊在现有多 Agent 编排基础上，接入科大讯飞 TTS 与文生图能力，生成“知识点播客化”音频和“知识配图”图片资产；同时为后续将 MiMo 大模型渐进迁移到科大讯飞星火/教育模型预留 Provider 层。
>
> 核心原则：不推倒重来、不替换现有 AI 问答主链路、不破坏 `AiClient` 注入方式、不改变资源工坊原有可用功能。新增能力必须作为资源工坊的可选增强项。

---

## 1. 目标与边界

### 1.1 本阶段目标

本阶段完成两个面向参赛展示的拓展能力：

1. 知识点“播客化”
   - 为每个资源包生成一段可播放的 MP3 音频解说。
   - 音频内容来自资源包已有产物：课程总结、知识点、学习路径、题目解析和多模态脚本。
   - 使用科大讯飞在线语音合成 TTS，由后端调用，前端只播放后端生成的文件。

2. 知识配图生成
   - 为资源包生成封面图。
   - 可选生成 1 到 3 张图文解析配图，用于重点知识点解释。
   - 使用科大讯飞文生图能力，由后端调用并保存图片文件。

3. 资源资产管理
   - 音频、图片都作为 `ResourceAsset` 保存元数据。
   - 前端资源工坊结果页展示音频播放器、图片画廊、生成状态、失败原因和下载入口。

4. 讯飞模型迁移准备
   - 当前阶段不替换 MiMo。
   - 在 `AiClient` 外围补充 Provider 设计，保证后续可以把总结、出题、学习计划、资源工坊中的 LLM 调用切换到讯飞星火/教育模型。

### 1.2 非目标

本阶段不做以下事情：

- 不替换现有 AI 问答页面主链路。
- 不把所有 AI 服务一次性迁移到讯飞模型。
- 不在浏览器端直连讯飞 API。
- 不新增复杂媒体编辑器。
- 不强依赖 ffmpeg 等本地二进制工具。
- 不要求一次生成长音频专辑；MVP 只生成单个资源包播客音频。

---

## 2. 官方能力约束

实现前必须再次核对讯飞官方文档，因为模型域名、套餐、参数和下线时间可能变化。本文按 2026-07-17 查询到的官方文档制定开发契约。

### 2.1 在线 TTS

参考文档：

- 科大讯飞在线语音合成 API：<https://www.xfyun.cn/doc/tts/online_tts/API.html>

关键约束：

- 推荐使用 WebSocket `wss://tts-api.xfyun.cn/v2/tts`。
- 后端使用 APPID、APIKey、APISecret 生成鉴权 URL，禁止把密钥暴露给前端。
- 单次请求文本小于 8000 字节，约 2000 个汉字；MVP 必须把播客脚本限制在 1800 个中文字符以内。
- 推荐生成 MP3：`aue=lame`，`sfl=1`。
- 响应中 `data.status = 2` 表示本次合成结束。

### 2.2 文生图

参考文档：

- 星火文生图 API：<https://www.xfyun.cn/doc/spark/ImageGeneration.html>
- HiDream 图片生成 API：<https://www.xfyun.cn/doc/spark/hidream.html>

默认选型：

- Phase X1 使用星火文生图同步接口作为 MVP：`https://spark-api.cn-huabei-1.xf-yun.com/v2.1/tti`。
- HiDream 作为后续高质量图片 Provider，可通过同一接口抽象接入异步任务创建与查询。

关键约束：

- 文生图接口不适合浏览器直连，必须由后端调用。
- 成功响应中图片内容以 base64 返回，后端解码后保存原始图片字节。
- 默认分辨率可用 512x512；封面图 MVP 使用 512x512 或 768x768，具体以当前套餐支持为准。
- 图片生成可能返回输入审核失败、生成结果审核失败等错误，必须映射为内容安全失败，不可重试刷屏。
- 官方说明图片会隐式添加 AI 生成内容标识，实现必须保存原始图片字节，不能二次压缩破坏标识。

### 2.3 星火/教育大模型迁移

参考文档：

- 星火 HTTP 调用文档：<https://www.xfyun.cn/doc/spark/HTTP%E8%B0%83%E7%94%A8%E6%96%87%E6%A1%A3.html>
- 星火自定义 API：<https://www.xfyun.cn/doc/spark/OptionalAPI.html>

本阶段只做架构预留：

- 不改 `AiClient.chat(...)` 和 `AiClient.chatStream(...)` 对外方法。
- 不修改 AI 问答主链路。
- 后续迁移时优先在 `AiClient` 内部或其下层 Provider 切换模型供应商。

---

## 3. 与现有代码的关系

### 3.1 必须复用的现有结构

| 现有模块 | 复用方式 |
|---|---|
| `GenerateResourcePackageRequest` | 增加可选字段，保持旧请求兼容 |
| `ResourceOrchestratorAgent` | 保持资源工坊主编排入口 |
| `MultimodalAgent` | 从“脚本生成 Agent”升级为“脚本 + 资产生成 Agent” |
| `ResourceGenerationTools` | 新增播客脚本、图片提示词和讯飞资产工具调用 |
| `ResourceAgentResult` | 增加 `assets` 字段，用于返回资源资产摘要 |
| `AiTaskService` | 继续负责异步任务、进度、取消检测和结果持久化 |
| `frontend/src/views/ResourceWorkshop.vue` | 在现有资源工坊页增加多模态资产展示 |
| `frontend/src/stores/task.js` | 继续作为唯一任务轮询入口 |

### 3.2 禁止事项

- 禁止在 `AiChat.vue` 中接入讯飞模型替换现有问答。
- 禁止 Controller 直接调用讯飞 SDK 或 HTTP API。
- 禁止 Agent 直接写文件。
- 禁止前端保存讯飞密钥。
- 禁止把音频和图片 base64 大字段塞进 `ai_task.result`。

---

## 4. 用户体验设计

### 4.1 资源工坊表单

在资源工坊生成表单中新增一个“多模态拓展”区域，位置放在资源类型选择之后、生成按钮之前。

字段：

| 字段 | 控件 | 默认值 | 说明 |
|---|---|---|---|
| `includePodcastAudio` | 开关 | false | 生成资源包音频解说 |
| `podcastStyle` | 下拉 | `teacher` | `teacher` 教师讲解、`review` 考前速记、`story` 轻播客 |
| `ttsVoice` | 下拉 | 后端默认 | 从能力接口返回可选声音，前端不可硬编码 |
| `includeKnowledgeImages` | 开关 | false | 生成封面和图文解析配图 |
| `imageCount` | 数字步进器 | 1 | 1 到 4，MVP 默认 1 |
| `imageStyle` | 下拉 | `clean_edu` | `clean_edu`、`diagram`、`blackboard`、`isometric` |

交互规则：

- 当后端能力接口返回 `tts.enabled=false` 时，音频开关禁用并显示“未配置讯飞 TTS”。
- 当后端能力接口返回 `image.enabled=false` 时，图片开关禁用并显示“未配置讯飞文生图”。
- 旧用户不勾选新开关时，资源工坊行为与当前版本完全一致。
- 不展示密钥、模型域名、鉴权细节。

### 4.2 生成中状态

沿用现有异步任务进度卡片，新增进度文案：

| 进度 | Agent | 文案 |
|---|---|---|
| 84 | MultimodalAgent | 正在整理多模态脚本 |
| 87 | MultimodalAgent | 正在生成播客讲稿 |
| 90 | MultimodalAgent | 正在合成音频解说 |
| 93 | MultimodalAgent | 正在生成知识配图 |
| 94 | MultimodalAgent | 正在保存多模态资源 |

约束：

- `ResourceOrchestratorAgent` 最后进度仍不得超过 94。
- `AiTaskService` 保存结果前仍使用 98。
- 如果用户取消任务，正在进行的网络请求无法强杀时，必须在下一步前停止写入新资产，并把任务标记为 `cancelled`。

### 4.3 结果展示

在 `ResourceWorkshop.vue` 结果区新增“多模态资产”区域，不做大面积营销式卡片，保持工作台风格。

展示内容：

1. 播客音频
   - 标题：资源包音频解说
   - HTML5 `<audio controls>`
   - 时长、声音、生成时间、下载按钮
   - 如果失败，展示简短失败原因和“重新生成”按钮

2. 知识配图
   - 封面图置于第一张，标记“封面”
   - 解析图按知识点标题展示
   - 每张图支持预览、下载、复制提示词摘要
   - 审核失败时展示“内容安全审核未通过”，不展示原始敏感提示词

3. 历史恢复
   - 从历史任务恢复资源包时，必须能恢复音频和图片资产列表。
   - 如果文件丢失，卡片状态显示“文件缺失”，不导致整个资源包无法打开。

---

## 5. 后端数据模型

### 5.1 数据库表

新增表 `resource_asset`，用于保存音频和图片资产元数据。文件本体保存到本地上传目录或后续对象存储。

```sql
CREATE TABLE IF NOT EXISTS resource_asset (
    id BIGSERIAL PRIMARY KEY,
    asset_id VARCHAR(64) NOT NULL UNIQUE,
    package_id VARCHAR(64) NOT NULL,
    task_id VARCHAR(64),
    user_id BIGINT NOT NULL,
    material_id BIGINT,
    asset_type VARCHAR(32) NOT NULL,
    asset_role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    provider VARCHAR(64) NOT NULL,
    model VARCHAR(128),
    file_name VARCHAR(255),
    content_type VARCHAR(128),
    storage_path VARCHAR(512),
    size_bytes BIGINT,
    duration_seconds INTEGER,
    width INTEGER,
    height INTEGER,
    prompt_summary TEXT,
    source_keys TEXT,
    error_code VARCHAR(64),
    error_message VARCHAR(512),
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_resource_asset_package_id ON resource_asset(package_id);
CREATE INDEX IF NOT EXISTS idx_resource_asset_task_id ON resource_asset(task_id);
CREATE INDEX IF NOT EXISTS idx_resource_asset_user_id ON resource_asset(user_id);
```

字段枚举：

| 字段 | 值 |
|---|---|
| `asset_type` | `audio`, `image` |
| `asset_role` | `podcast`, `cover`, `explanation` |
| `status` | `pending`, `generating`, `success`, `failed`, `cancelled` |
| `provider` | `xfyun-tts`, `xfyun-tti`, `xfyun-hidream` |

### 5.2 Entity / DTO / VO

新增后端类：

```text
backend/src/main/java/com/study/entity/ResourceAsset.java
backend/src/main/java/com/study/mapper/ResourceAssetMapper.java
backend/src/main/java/com/study/dto/vo/ResourceAssetVO.java
backend/src/main/java/com/study/dto/vo/ResourceAssetCapabilitiesVO.java
backend/src/main/java/com/study/dto/request/RetryResourceAssetRequest.java
```

`ResourceAssetVO` 字段：

```java
private String assetId;
private String packageId;
private String taskId;
private Long materialId;
private String assetType;
private String assetRole;
private String status;
private String provider;
private String model;
private String fileName;
private String contentType;
private Long sizeBytes;
private Integer durationSeconds;
private Integer width;
private Integer height;
private String promptSummary;
private String errorCode;
private String errorMessage;
private String previewUrl;
private String downloadUrl;
private LocalDateTime createdAt;
```

`ResourceAssetCapabilitiesVO` 字段：

```java
private Boolean ttsEnabled;
private Boolean imageEnabled;
private List<VoiceOption> voices;
private List<String> imageStyles;
private Integer maxImageCount;
private String defaultVoice;
private String defaultImageStyle;
```

---

## 6. 配置规范

新增配置类：

```text
backend/src/main/java/com/study/config/XfyunProperties.java
backend/src/main/java/com/study/config/ResourceAssetProperties.java
```

`application.yml` 增加公共配置：

```yaml
xfyun:
  app-id: ${XFYUN_APP_ID:}
  api-key: ${XFYUN_API_KEY:}
  api-secret: ${XFYUN_API_SECRET:}
  tts:
    enabled: ${XFYUN_TTS_ENABLED:false}
    endpoint: ${XFYUN_TTS_ENDPOINT:wss://tts-api.xfyun.cn/v2/tts}
    voice: ${XFYUN_TTS_VOICE:xiaoyan}
    sample-rate: ${XFYUN_TTS_SAMPLE_RATE:16000}
    format: mp3
    timeout-ms: ${XFYUN_TTS_TIMEOUT_MS:60000}
    max-text-bytes: 7600
  image:
    enabled: ${XFYUN_IMAGE_ENABLED:false}
    provider: ${XFYUN_IMAGE_PROVIDER:tti}
    endpoint: ${XFYUN_IMAGE_ENDPOINT:https://spark-api.cn-huabei-1.xf-yun.com/v2.1/tti}
    model: ${XFYUN_IMAGE_MODEL:spark-tti}
    timeout-ms: ${XFYUN_IMAGE_TIMEOUT_MS:60000}
    max-image-count: 4
    default-size: 512x512

resource-asset:
  storage-dir: ${RESOURCE_ASSET_STORAGE_DIR:${file.upload-dir}/resource-assets}
  public-url-prefix: ${RESOURCE_ASSET_PUBLIC_URL_PREFIX:/api/ai/resource-assets}
  max-audio-bytes: 20971520
  max-image-bytes: 10485760
```

配置规则：

- 任何密钥只允许通过环境变量注入。
- `enabled=true` 但密钥不完整时，应用启动不失败，但能力接口返回 disabled，并给出内部日志。
- 前端只读取 `/ai/resource-assets/capabilities`，不可读取配置文件。

---

## 7. Provider 与服务设计

### 7.1 包结构

```text
backend/src/main/java/com/study/ai/multimodal/
├── model/
│   ├── AssetGenerationRequest.java
│   ├── AssetGenerationResult.java
│   ├── AudioSynthesisRequest.java
│   ├── ImageGenerationRequest.java
│   └── ProviderErrorType.java
├── provider/
│   ├── SpeechSynthesisProvider.java
│   ├── ImageGenerationProvider.java
│   ├── xfyun/
│   │   ├── XfyunSignatureUtil.java
│   │   ├── XfyunTtsProvider.java
│   │   ├── XfyunImageGenerationProvider.java
│   │   └── XfyunHiDreamImageProvider.java
├── storage/
│   └── ResourceAssetStorageService.java
└── service/
    ├── ResourceAssetService.java
    └── ResourceAssetConverter.java
```

### 7.2 接口契约

`SpeechSynthesisProvider`：

```java
public interface SpeechSynthesisProvider {
    /**
     * Synthesizes the provided narration text into an audio asset.
     */
    AssetGenerationResult synthesize(AudioSynthesisRequest request);

    /**
     * Returns true when provider configuration is complete and enabled.
     */
    boolean isAvailable();
}
```

`ImageGenerationProvider`：

```java
public interface ImageGenerationProvider {
    /**
     * Generates one or more image assets from the supplied prompt.
     */
    List<AssetGenerationResult> generate(ImageGenerationRequest request);

    /**
     * Returns true when provider configuration is complete and enabled.
     */
    boolean isAvailable();
}
```

`AssetGenerationResult` 必须包含：

```java
private boolean success;
private byte[] content;
private String contentType;
private String provider;
private String model;
private String fileExtension;
private Integer width;
private Integer height;
private Integer durationSeconds;
private String errorCode;
private String errorMessage;
private ProviderErrorType errorType;
private Map<String, Object> metadata;
```

`ProviderErrorType`：

```java
AUTH_FAILED
CONFIG_MISSING
RATE_LIMITED
CONTENT_REJECTED
TEXT_TOO_LONG
TIMEOUT
REMOTE_ERROR
INVALID_RESPONSE
UNKNOWN
```

### 7.3 资产服务

`ResourceAssetService` 职责：

- 创建 pending 资产记录。
- 调用 Provider 生成内容。
- 调用 `ResourceAssetStorageService` 保存文件。
- 更新资产状态。
- 查询当前用户可访问的资源包资产。
- 生成下载响应。
- 重试失败资产。

约束：

- Service 必须校验 `userId`，禁止跨用户读取资产。
- 文件名使用 UUID，不使用用户输入。
- `storagePath` 保存相对路径，不保存绝对路径到前端。
- 下载接口通过后端读取文件返回，不直接暴露磁盘路径。

---

## 8. 资源工坊 Agent 改造

### 8.1 请求 DTO 扩展

修改 `GenerateResourcePackageRequest`，新增可选字段：

```java
private Boolean includePodcastAudio;
private String podcastStyle;
private String ttsVoice;
private Boolean includeKnowledgeImages;
private Integer imageCount;
private String imageStyle;
```

默认值规则：

| 字段 | null 语义 |
|---|---|
| `includePodcastAudio` | false |
| `podcastStyle` | `teacher` |
| `ttsVoice` | 使用后端默认声音 |
| `includeKnowledgeImages` | false |
| `imageCount` | 1 |
| `imageStyle` | `clean_edu` |

参数校验：

- `imageCount` 范围 1 到 `xfyun.image.max-image-count`。
- `podcastStyle` 只允许白名单值。
- `imageStyle` 只允许白名单值。
- `ttsVoice` 只能是能力接口返回的 voice id，或为空。

### 8.2 Context 扩展

修改 `ResourceAgentContext`，新增：

```java
private Boolean includePodcastAudio;
private String podcastStyle;
private String ttsVoice;
private Boolean includeKnowledgeImages;
private Integer imageCount;
private String imageStyle;
private List<ResourceAssetVO> assets;
```

`ResourceOrchestratorAgent` 初始化 Context 时写入这些字段。

### 8.3 Result 扩展

修改 `ResourceAgentResult`，新增：

```java
private List<ResourceAssetVO> assets;
```

返回 Map 时新增：

```json
{
  "assets": [
    {
      "assetId": "asset_xxx",
      "assetType": "audio",
      "assetRole": "podcast",
      "status": "success",
      "previewUrl": "/api/ai/resource-assets/asset_xxx/download",
      "downloadUrl": "/api/ai/resource-assets/asset_xxx/download"
    }
  ]
}
```

旧历史任务没有 `assets` 字段时，前端按空数组处理。

### 8.4 MultimodalAgent 升级

`MultimodalAgent` 仍然先生成当前已有的多模态脚本资源，不删除原功能。

新增流程：

1. 如果 `includePodcastAudio=true`
   - 调用 `ResourceGenerationTools.generatePodcastScript(context)` 生成播客脚本。
   - 控制脚本长度小于 `xfyun.tts.max-text-bytes`。
   - 调用 `ResourceAssetService.generatePodcastAsset(...)`。
   - 将返回的 `ResourceAssetVO` 加入 Context 和 Result。

2. 如果 `includeKnowledgeImages=true`
   - 调用 `ResourceGenerationTools.generateImagePrompts(context)` 生成封面和解析图提示词。
   - 调用 `ResourceAssetService.generateImageAssets(...)`。
   - 将返回的 `ResourceAssetVO` 加入 Context 和 Result。

3. 如果 Provider 未启用
   - 不让整个资源包失败。
   - 生成一个 failed 资产记录，错误码为 `CONFIG_MISSING`。
   - AgentStep 状态为 `SUCCESS`，但 `outputSummary` 说明多模态资产部分失败。

4. 如果音频/图片生成全部失败
   - 不影响总结、题库、学习路径等主资源。
   - `ResourceAgentResult.status` 可保持 `partial_success`。
   - `qualityChecks` 中追加“多模态资产生成失败，需要检查讯飞配置或内容审核”。

### 8.5 播客脚本生成规则

新增 Prompt 模板：

```text
backend/src/main/java/com/study/ai/prompt/PromptTemplates.java
```

方法：

```java
public static String buildPodcastScriptPrompt(ResourceAgentContext context)
```

输出要求：

- Markdown 或纯文本均可，但最终传给 TTS 前必须去除 Markdown 标记。
- 中文口播风格，自然、简洁。
- 必须包含：
  - 课程/资料名称；
  - 3 到 5 个核心知识点；
  - 1 个学习提醒；
  - 1 个练习建议。
- 禁止生成虚构事实。
- 长度控制在 1200 到 1800 中文字符。

TTS 前清洗：

- 删除 Markdown 标题符号、代码块、表格符号。
- 替换难读符号，例如 `->` 改为“到”，`API` 可保留。
- 连续空白压缩为一个中文逗号或句号。

### 8.6 图片提示词生成规则

新增方法：

```java
public static String buildImagePromptGenerationPrompt(ResourceAgentContext context)
```

LLM 生成结构化 JSON：

```json
{
  "cover": {
    "title": "资源包封面",
    "prompt": "清晰教育风格封面，主题是...",
    "negativePrompt": "低清晰度，错误文字，杂乱布局"
  },
  "explanations": [
    {
      "knowledgePoint": "Transformer 注意力机制",
      "prompt": "用简洁示意图解释...",
      "negativePrompt": "错误公式，乱码文字"
    }
  ]
}
```

提示词约束：

- 不要求模型在图中生成大量文字，避免乱码。
- 配图应服务于学习理解，不生成纯装饰图。
- 不使用名人肖像、品牌 Logo、真实学校标识。
- 对敏感材料，提示词应抽象成教育图解，不复述敏感内容。

---

## 9. Controller/API 设计

### 9.1 能力查询

```http
GET /api/ai/resource-assets/capabilities
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "ttsEnabled": true,
    "imageEnabled": true,
    "voices": [
      { "id": "xiaoyan", "name": "讯飞小燕", "gender": "female" }
    ],
    "imageStyles": ["clean_edu", "diagram", "blackboard", "isometric"],
    "maxImageCount": 4,
    "defaultVoice": "xiaoyan",
    "defaultImageStyle": "clean_edu"
  }
}
```

### 9.2 查询资源包资产

```http
GET /api/ai/resource-packages/{packageId}/assets
```

规则：

- 只返回当前用户拥有的资产。
- `packageId` 来自资源工坊结果中的 `packageId`。
- 如果 package 不存在或不属于当前用户，返回 404 或空列表，按项目现有权限风格实现。

### 9.3 下载资产

```http
GET /api/ai/resource-assets/{assetId}/download
```

规则：

- 校验用户权限。
- `Content-Type` 使用资产记录中的类型。
- `Content-Disposition` 使用安全文件名。
- 文件不存在时返回 404，并把资产状态更新为 `failed`，错误码 `FILE_MISSING`。

### 9.4 重试资产

```http
POST /api/ai/resource-assets/{assetId}/retry
```

规则：

- 只允许重试 `failed` 状态资产。
- 复用原始 `promptSummary` 或 `metadata.originalPrompt`。
- 重试成功后更新原资产记录，不新建资产。
- 如果原始 prompt 缺失，返回 400。

MVP 可先实现后端接口，前端按钮仅在 `status=failed` 时显示。

---

## 10. 前端改造清单

### 10.1 API 封装

修改 `frontend/src/api/ai.js`：

```js
export async function getResourceAssetCapabilities() {
  const res = await api.get('/ai/resource-assets/capabilities')
  return res.data
}

export async function listResourcePackageAssets(packageId) {
  const res = await api.get(`/ai/resource-packages/${packageId}/assets`)
  return res.data
}

export async function retryResourceAsset(assetId) {
  const res = await api.post(`/ai/resource-assets/${assetId}/retry`)
  return res.data
}
```

生成资源包时，`generateResourcePackageAsync(params)` 透传新增字段。

### 10.2 组件拆分

新增组件：

```text
frontend/src/components/resource-assets/
├── ResourceAssetPanel.vue
├── PodcastAssetCard.vue
├── ImageAssetGallery.vue
└── AssetStatusBadge.vue
```

职责：

- `ResourceAssetPanel.vue`：聚合音频和图片资产。
- `PodcastAssetCard.vue`：音频播放器、下载、状态。
- `ImageAssetGallery.vue`：图片网格、预览、下载、失败状态。
- `AssetStatusBadge.vue`：统一状态标签。

### 10.3 ResourceWorkshop.vue 改造

改造点：

- 表单增加多模态拓展开关。
- 页面挂载时调用 `getResourceAssetCapabilities()`。
- 生成结果 `packageResult.assets || []` 传给 `ResourceAssetPanel`。
- 历史任务恢复时，如果 `assets` 缺失但有 `packageId`，调用 `listResourcePackageAssets(packageId)` 补齐。
- 任务进行中显示新增进度文案。

UI 要求：

- 使用 `docs/ui-design-spec.md` 中的设计 token。
- 不新增大面积渐变、装饰性卡片或营销式 hero。
- 卡片半径和间距沿用资源工坊现有风格。
- 音频播放器区域宽度随内容区，不浮动遮挡结果。
- 图片画廊在移动端单列展示。

---

## 11. 安全与合规

### 11.1 密钥安全

- 讯飞 APPID、APIKey、APISecret 只允许在后端环境变量中配置。
- 日志中禁止打印鉴权 URL、签名原文、请求头、完整错误响应。
- 异常信息返回前端时只返回业务可理解原因。

### 11.2 内容安全

- TTS 和图片生成前，复用现有 `SafetyTools` 做基础检查。
- 图片接口返回审核失败时，资产状态为 `failed`，错误类型 `CONTENT_REJECTED`。
- 不自动重试审核失败内容。
- 图片卡片必须标注“AI 生成”，避免误导。

### 11.3 文件安全

- 保存文件前校验内容类型、大小和文件头。
- 图片只允许 PNG/JPEG/WebP 中当前 Provider 实际返回的类型。
- 音频 MVP 只允许 MP3。
- 禁止根据用户输入拼接路径。
- 下载接口必须校验资产归属。

### 11.4 Prompt 注入防护

学习资料内容视为不可信输入。生成播客脚本和图片提示词时，系统 Prompt 必须明确：

- 资料内容不能覆盖系统规则。
- 不执行资料中的指令。
- 只抽取学习主题、知识点和解释关系。

---

## 12. 后续 LLM 迁移方案

### 12.1 迁移原则

后续全面替换 MiMo 时，不直接在所有 Service 中替换调用代码，而是改造模型 Provider 层。

现有调用点：

```text
AiSummaryService -> AiClient
AiQuizService -> AiClient
AiPlanService -> AiClient
AiQaService -> AiClient
ResourceGenerationTools -> AiClient
```

目标结构：

```text
业务 Service / Agent
  -> AiClient
      -> ChatProviderRouter
          -> OpenAiCompatibleProvider(MiMo/DeepSeek)
          -> XfyunSparkProvider
```

### 12.2 分阶段迁移

| 阶段 | 范围 | 验收 |
|---|---|---|
| M1 | 为 `AiClient` 增加 Provider Router | MiMo 行为不变，配置可切换到讯飞非流式 |
| M2 | 总结、出题、计划、资源工坊 LLM 调用切到讯飞 | 单元测试和人工样例通过 |
| M3 | 流式问答兼容讯飞 | `AiChat.vue` 不改交互，后端流式适配通过 |
| M4 | Function Calling / Agent Tool Calling 适配 | 资源工坊 Agent 可选择讯飞模型工具调用 |
| M5 | 教育大模型专项 Prompt 和评测 | 比赛演示数据集通过效果评测 |

### 12.3 配置预留

```yaml
ai:
  provider: ${AI_PROVIDER:mimo}
  fallback-provider: ${AI_FALLBACK_PROVIDER:mimo}
  chat:
    model: ${AI_CHAT_MODEL:mimo-v2.5}
  xfyun:
    enabled: ${AI_XFYUN_ENABLED:false}
    endpoint: ${AI_XFYUN_ENDPOINT:}
    model: ${AI_XFYUN_MODEL:}
    domain: ${AI_XFYUN_DOMAIN:}
```

说明：

- 讯飞星火不同版本的 endpoint、domain、model 参数可能不同，具体值必须以实现当天官方文档为准。
- 迁移时保留 MiMo fallback，避免比赛演示当天因额度或网络问题整体不可用。

---

## 13. 测试方案

### 13.1 单元测试

后端新增测试：

| 测试类 | 覆盖点 |
|---|---|
| `XfyunSignatureUtilTest` | HMAC 签名参数稳定、URL 编码正确 |
| `XfyunTtsProviderTest` | 成功响应拼接、结束帧识别、错误码映射 |
| `XfyunImageGenerationProviderTest` | base64 解码、审核失败映射、非法响应处理 |
| `ResourceAssetStorageServiceTest` | 路径穿越防护、大小限制、文件头校验 |
| `ResourceAssetServiceTest` | pending/success/failed 状态流转、权限过滤 |
| `MultimodalAgentAssetTest` | 开关关闭不调用 Provider、开关打开写入 assets、Provider 失败不拖垮资源包 |
| `ResourceAssetControllerTest` | 能力查询、列表、下载、重试权限 |

测试要求：

- 单元测试不能调用真实讯飞网络。
- Provider 使用 mock 或本地 fake server。
- 真实联调用例放在手工验收，不纳入默认 CI。

### 13.2 前端测试

至少验证：

- `npm run build` 通过。
- 能力 disabled 时开关禁用。
- 历史任务无 `assets` 字段不报错。
- 音频成功时显示播放器。
- 图片成功时显示画廊。
- failed 资产显示失败原因和重试入口。

### 13.3 手工验收

准备一份 5 到 10 页课程资料，执行：

1. 不勾选多模态拓展，资源包生成结果与当前版本一致。
2. 勾选播客音频，生成完成后可以在线播放 MP3。
3. 勾选知识配图，生成完成后可以看到封面图。
4. 关闭讯飞配置，前端开关禁用或资产失败可解释。
5. 触发图片审核失败样例，主资源包仍可用。
6. 历史记录恢复后，音频和图片仍能展示或下载。

---

## 14. 实施顺序

建议按以下顺序实现，避免一次性大改：

1. 新增配置类和能力查询接口。
2. 新增 `resource_asset` 表、Entity、Mapper、VO、Converter。
3. 新增文件存储服务和资产下载接口。
4. 新增 Provider 接口和 fake/mock 实现，先打通业务链路。
5. 改造 `GenerateResourcePackageRequest`、`ResourceAgentContext`、`ResourceAgentResult`。
6. 改造 `MultimodalAgent`，接入资产生成但先使用 fake provider。
7. 前端增加表单开关和资产展示组件。
8. 接入真实 `XfyunTtsProvider`。
9. 接入真实 `XfyunImageGenerationProvider`。
10. 增加重试接口和前端重试按钮。
11. 补齐单元测试、前端 build、手工验收记录。
12. 最后更新 `.claude/task-list.md` 和演示文档。

---

## 15. 验收标准

功能验收：

- 资源工坊可选择是否生成播客音频。
- 资源工坊可选择是否生成知识配图。
- 生成结果中有 `assets` 数组。
- 音频资产可在线播放和下载。
- 图片资产可预览和下载。
- Provider 失败不会导致总结、题库、学习路径丢失。
- 历史任务可恢复多模态资产展示。

工程验收：

- 后端编译通过。
- 后端新增测试通过。
- 前端 `npm run build` 通过。
- 不修改 AI 问答主链路。
- 不泄露讯飞密钥。
- 新增公共 Java 方法包含 JavaDoc。
- API 文档、任务清单同步更新。

比赛展示验收：

- PPT 可以说明“科大讯飞 TTS + 文生图 + 多 Agent 资源工坊”的闭环。
- 演示视频中可以展示资源包生成后直接播放音频、查看封面/配图。
- 文档中能明确标注使用的讯飞能力、接口来源和安全合规处理。
