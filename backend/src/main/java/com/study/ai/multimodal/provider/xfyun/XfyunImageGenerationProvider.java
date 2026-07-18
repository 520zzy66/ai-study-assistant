package com.study.ai.multimodal.provider.xfyun;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.ai.multimodal.model.AssetGenerationResult;
import com.study.ai.multimodal.model.ImageGenerationRequest;
import com.study.ai.multimodal.model.ProviderErrorType;
import com.study.ai.multimodal.provider.ImageGenerationProvider;
import com.study.config.XfyunProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 科大讯飞星火文生图同步 Provider 实现。
 *
 * <p>实现细节：
 * <ul>
 *   <li>使用 JDK 内置 {@link java.net.http.HttpClient}，无需额外依赖。</li>
 *   <li>鉴权 URL：authorization/date/host query 参数由 HMAC-SHA256 签名生成。</li>
 *   <li>请求体：header.app_id + parameter.chat.domain/width/height + payload.message.text。</li>
 *   <li>响应体：payload.choices.text[0].content (Base64 编码图片字节)。</li>
 *   <li>审核失败映射为 {@link ProviderErrorType#CONTENT_REJECTED}，不可重试。</li>
 * </ul>
 *
 * <p>安全约束：日志中不打印鉴权头、原始提示词和 base64 图片字节。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class XfyunImageGenerationProvider implements ImageGenerationProvider {

    private static final String PROVIDER_NAME = "xfyun-tti";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final XfyunProperties xfyunProperties;

    @Override
    public List<AssetGenerationResult> generate(ImageGenerationRequest request) {
        if ("hidream".equalsIgnoreCase(xfyunProperties.getImage().getProvider())) {
            return new XfyunHiDreamImageProvider(xfyunProperties).generate(request);
        }
        if (!isAvailable()) {
            return Collections.singletonList(AssetGenerationResult.failure(PROVIDER_NAME,
                    ProviderErrorType.CONFIG_MISSING, "CONFIG_MISSING", "讯飞文生图未配置或未启用"));
        }
        if (request.getPrompt() == null || request.getPrompt().isBlank()) {
            return Collections.singletonList(AssetGenerationResult.failure(PROVIDER_NAME,
                    ProviderErrorType.INVALID_RESPONSE, "EMPTY_PROMPT", "图片提示词为空"));
        }
        try {
            AssetGenerationResult result = doGenerate(request);
            return Collections.singletonList(result);
        } catch (Exception e) {
            log.warn("Xfyun image generation failed: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return Collections.singletonList(AssetGenerationResult.failure(PROVIDER_NAME,
                    mapException(e), "REMOTE_ERROR", "讯飞文生图调用失败: " + e.getMessage()));
        }
    }

    @Override
    public boolean isAvailable() {
        return xfyunProperties.isImageConfigured();
    }

    /**
     * 执行 HTTP 同步调用并解析 base64 图片字节。
     */
    private AssetGenerationResult doGenerate(ImageGenerationRequest request) throws Exception {
        int width = request.getWidth() != null ? request.getWidth() : 512;
        int height = request.getHeight() != null ? request.getHeight() : 512;

        Map<String, Object> body = buildRequestBody(request, width, height);
        String bodyJson = MAPPER.writeValueAsString(body);

        String authUrl = XfyunSignatureUtil.buildHttpAuthUrl(
                xfyunProperties.getImage().getEndpoint(),
                "POST",
                xfyunProperties.getImageApiKey(),
                xfyunProperties.getImageApiSecret());

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(xfyunProperties.getImage().getTimeoutMs()))
                .sslContext(SslUtil.createTrustAllContext())
                .build();

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(authUrl))
                .header("Content-Type", "application/json;charset=UTF-8")
                .timeout(Duration.ofMillis(xfyunProperties.getImage().getTimeoutMs()))
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int statusCode = response.statusCode();
        String responseBody = response.body();

        if (statusCode < 200 || statusCode >= 300) {
            return parseErrorResponse(responseBody, statusCode);
        }

        return parseSuccessResponse(responseBody, request, width, height);
    }

    /**
     * 构造讯飞文生图请求体。
     */
    private Map<String, Object> buildRequestBody(ImageGenerationRequest request, int width, int height) {
        Map<String, Object> body = new HashMap<>();

        Map<String, Object> header = new HashMap<>();
        header.put("app_id", xfyunProperties.getImageAppId());
        body.put("header", header);

        Map<String, Object> chat = new HashMap<>();
        chat.put("domain", "general");
        chat.put("width", width);
        chat.put("height", height);
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("chat", chat);
        body.put("parameter", parameter);

        // 拼接提示词与风格
        StringBuilder textBuilder = new StringBuilder();
        if (request.getImageStyle() != null && !request.getImageStyle().isBlank()) {
            textBuilder.append("风格：").append(styleDescription(request.getImageStyle())).append("。");
        }
        textBuilder.append(request.getPrompt());
        if (request.getNegativePrompt() != null && !request.getNegativePrompt().isBlank()) {
            textBuilder.append("。避免：").append(request.getNegativePrompt());
        }

        Map<String, Object> messageText = new HashMap<>();
        messageText.put("role", "user");
        messageText.put("content", textBuilder.toString());
        Map<String, Object> message = new HashMap<>();
        message.put("text", Collections.singletonList(messageText));
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", message);
        body.put("payload", payload);

        return body;
    }

    /**
     * 将图片风格枚举映射为中文描述（写入提示词）。
     */
    private String styleDescription(String style) {
        return switch (style) {
            case "clean_edu" -> "清晰教育插画";
            case "diagram" -> "结构化图解";
            case "blackboard" -> "黑板手绘";
            case "isometric" -> "等距风格";
            default -> style;
        };
    }

    /**
     * 解析成功响应，提取 base64 图片字节。
     */
    private AssetGenerationResult parseSuccessResponse(String responseBody, ImageGenerationRequest request,
                                                       int width, int height) throws Exception {
        JsonNode root = MAPPER.readTree(responseBody);
        int code = root.path("header").path("code").asInt(-1);
        if (code != 0) {
            String message = root.path("header").path("message").asText("unknown");
            // 10021/10022 为官方图片生成审核错误码，兼容保留历史测试中的 10040/10043。
            ProviderErrorType errorType = (code == 10021 || code == 10022
                    || code == 10043 || code == 10040 || message.contains("审核"))
                    ? ProviderErrorType.CONTENT_REJECTED
                    : ProviderErrorType.REMOTE_ERROR;
            return AssetGenerationResult.failure(PROVIDER_NAME, errorType,
                    String.valueOf(code), "讯飞文生图返回错误");
        }

        JsonNode textArray = root.path("payload").path("choices").path("text");
        if (!textArray.isArray() || textArray.isEmpty()) {
            return AssetGenerationResult.failure(PROVIDER_NAME, ProviderErrorType.INVALID_RESPONSE,
                    "EMPTY_IMAGE", "响应中未包含图片数据");
        }

        String base64Content = textArray.get(0).path("content").asText("");
        if (base64Content.isEmpty()) {
            return AssetGenerationResult.failure(PROVIDER_NAME, ProviderErrorType.INVALID_RESPONSE,
                    "EMPTY_IMAGE", "响应图片内容为空");
        }

        byte[] imageBytes;
        try {
            imageBytes = Base64.getDecoder().decode(base64Content);
        } catch (IllegalArgumentException e) {
            return AssetGenerationResult.failure(PROVIDER_NAME, ProviderErrorType.INVALID_RESPONSE,
                    "INVALID_BASE64", "图片 base64 解码失败");
        }

        if (imageBytes.length == 0) {
            return AssetGenerationResult.failure(PROVIDER_NAME, ProviderErrorType.INVALID_RESPONSE,
                    "EMPTY_IMAGE", "图片字节为空");
        }

        // 推断图片格式（默认 png，必要时根据 magic bytes 推断）
        String contentType = detectContentType(imageBytes);
        String fileExtension = contentType.endsWith("png") ? "png"
                : contentType.endsWith("jpeg") ? "jpg" : "png";

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("width", width);
        metadata.put("height", height);
        metadata.put("sizeBytes", imageBytes.length);
        metadata.put("assetRole", request.getAssetRole() != null ? request.getAssetRole() : "explanation");

        return AssetGenerationResult.builder()
                .success(true)
                .provider(PROVIDER_NAME)
                .model(xfyunProperties.getImage().getModel())
                .content(imageBytes)
                .contentType(contentType)
                .fileExtension(fileExtension)
                .width(width)
                .height(height)
                .metadata(metadata)
                .build();
    }

    /**
     * 解析错误响应。
     */
    private AssetGenerationResult parseErrorResponse(String responseBody, int statusCode) {
        String errorCode = "HTTP_" + statusCode;
        String errorMessage = "HTTP 状态码异常";
        ProviderErrorType errorType = ProviderErrorType.REMOTE_ERROR;
        if (statusCode == 401 || statusCode == 403) {
            errorType = ProviderErrorType.AUTH_FAILED;
            errorMessage = "鉴权失败";
        } else if (statusCode == 429) {
            errorType = ProviderErrorType.RATE_LIMITED;
            errorMessage = "触发限流";
        } else if (statusCode == 400) {
            // 400 可能是内容审核失败
            errorType = ProviderErrorType.CONTENT_REJECTED;
            errorMessage = "请求被拒绝";
        }
        // 尝试从响应体中提取审核失败语义
        if (responseBody != null && (responseBody.contains("审核") || responseBody.contains("sensitive"))) {
            errorType = ProviderErrorType.CONTENT_REJECTED;
            errorMessage = "内容安全审核未通过";
        }
        log.warn("Xfyun image generation HTTP error: status={}", statusCode);
        return AssetGenerationResult.failure(PROVIDER_NAME, errorType, errorCode, errorMessage);
    }

    /**
     * 根据文件头识别图片类型。
     */
    private String detectContentType(byte[] bytes) {
        if (bytes.length < 4) {
            return "image/png";
        }
        // PNG: 89 50 4E 47
        if ((bytes[0] & 0xFF) == 0x89 && (bytes[1] & 0xFF) == 0x50
                && (bytes[2] & 0xFF) == 0x4E && (bytes[3] & 0xFF) == 0x47) {
            return "image/png";
        }
        // JPEG: FF D8 FF
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        // WebP: 52 49 46 46 ... 57 45 42 50
        if (bytes.length >= 12
                && (bytes[0] & 0xFF) == 0x52 && (bytes[1] & 0xFF) == 0x49
                && (bytes[2] & 0xFF) == 0x46 && (bytes[3] & 0xFF) == 0x46
                && (bytes[8] & 0xFF) == 0x57 && (bytes[9] & 0xFF) == 0x45
                && (bytes[10] & 0xFF) == 0x42 && (bytes[11] & 0xFF) == 0x50) {
            return "image/webp";
        }
        return "image/png";
    }

    /**
     * 将异常映射为 ProviderErrorType。
     */
    private ProviderErrorType mapException(Exception e) {
        if (e instanceof java.util.concurrent.TimeoutException) {
            return ProviderErrorType.TIMEOUT;
        }
        if (e instanceof java.net.ConnectException || e instanceof java.net.SocketTimeoutException) {
            return ProviderErrorType.REMOTE_ERROR;
        }
        return ProviderErrorType.UNKNOWN;
    }
}
