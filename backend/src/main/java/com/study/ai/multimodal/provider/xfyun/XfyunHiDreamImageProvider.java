package com.study.ai.multimodal.provider.xfyun;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.ai.multimodal.model.AssetGenerationResult;
import com.study.ai.multimodal.model.ImageGenerationRequest;
import com.study.ai.multimodal.model.ProviderErrorType;
import com.study.ai.multimodal.provider.ImageGenerationProvider;
import com.study.config.XfyunProperties;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 讯飞 HiDream 图片生成 Provider。
 *
 * <p>HiDream 为异步任务接口，需要先创建任务再轮询查询。
 *
 * @see <a href="https://www.xfyun.cn/doc/spark/hidream.html">HiDream 图片生成 API</a>
 */
@Slf4j
public class XfyunHiDreamImageProvider implements ImageGenerationProvider {

    private static final String PROVIDER_NAME = "xfyun-hidream";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final XfyunProperties properties;

    public XfyunHiDreamImageProvider(XfyunProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<AssetGenerationResult> generate(ImageGenerationRequest request) {
        if (!isAvailable()) {
            return Collections.singletonList(AssetGenerationResult.failure(PROVIDER_NAME,
                    ProviderErrorType.CONFIG_MISSING, "CONFIG_MISSING", "讯飞 HiDream 未配置或未启用"));
        }
        if (request == null || request.getPrompt() == null || request.getPrompt().isBlank()) {
            return Collections.singletonList(AssetGenerationResult.failure(PROVIDER_NAME,
                    ProviderErrorType.INVALID_RESPONSE, "EMPTY_PROMPT", "图片提示词为空"));
        }
        try {
            return Collections.singletonList(doGenerate(request));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.singletonList(AssetGenerationResult.failure(PROVIDER_NAME,
                    ProviderErrorType.TIMEOUT, "INTERRUPTED", "HiDream 图片任务被中断"));
        } catch (Exception e) {
            log.warn("HiDream image generation failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            return Collections.singletonList(AssetGenerationResult.failure(PROVIDER_NAME,
                    mapException(e), "REMOTE_ERROR", "HiDream 图片生成调用失败"));
        }
    }

    @Override
    public boolean isAvailable() {
        return properties.isImageConfigured()
                && properties.getImage().getEndpoint() != null
                && !properties.getImage().getEndpoint().isBlank();
    }

    private AssetGenerationResult doGenerate(ImageGenerationRequest request) throws Exception {
        XfyunProperties.Image image = properties.getImage();
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(image.getTimeoutMs()))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .version(HttpClient.Version.HTTP_1_1)
                .sslContext(SslUtil.createTrustAllContext())
                .build();
        long deadline = System.nanoTime() + Duration.ofMillis(image.getTimeoutMs()).toNanos();

        Map<String, Object> imageText = new HashMap<>();
        imageText.put("prompt", limit(buildPrompt(request), 2000));
        imageText.put("negative_prompt", limit(request.getNegativePrompt(), 2000));
        imageText.put("aspect_ratio", aspectRatio(request));
        imageText.put("img_count", 1);
        imageText.put("resolution", "2k");
        String encodedText = Base64.getEncoder().encodeToString(
                MAPPER.writeValueAsBytes(imageText));

        Map<String, Object> body = new HashMap<>();
        body.put("header", Map.of("app_id", properties.getImageAppId(), "status", 3,
                "channel", "default", "callback_url", "default"));
        body.put("parameter", Map.of("oig", Map.of("result", Map.of(
                "encoding", "utf8", "compress", "raw", "format", "json"))));
        body.put("payload", Map.of("oig", Map.of("encoding", "utf8", "compress", "raw",
                "format", "json", "status", 3, "text", encodedText)));

        String createBody = MAPPER.writeValueAsString(body);
        long createTimeoutMs = Math.max(1000L, Duration.ofNanos(deadline - System.nanoTime()).toMillis());
        JsonNode created = sendJson(client, image.getEndpoint(), createBody,
                Math.min(image.getTimeoutMs(), createTimeoutMs));
        int createCode = created.path("header").path("code").asInt(-1);
        if (createCode != 0) {
            return failureForCode(createCode, created.path("header").path("message").asText());
        }
        String taskId = created.path("header").path("task_id").asText("");
        if (taskId.isBlank()) {
            return AssetGenerationResult.failure(PROVIDER_NAME, ProviderErrorType.INVALID_RESPONSE,
                    "EMPTY_TASK_ID", "HiDream 未返回任务 ID");
        }

        while (System.nanoTime() < deadline) {
            long remainingMs = Math.max(1000L, Duration.ofNanos(deadline - System.nanoTime()).toMillis());
            JsonNode queried = sendJson(client, queryEndpoint(), MAPPER.writeValueAsString(
                    Map.of("header", Map.of("app_id", properties.getImageAppId(), "task_id", taskId))),
                    Math.min(image.getTimeoutMs(), remainingMs));
            int code = queried.path("header").path("code").asInt(-1);
            if (code != 0) {
                return failureForCode(code, queried.path("header").path("message").asText());
            }
            String status = queried.path("header").path("task_status").asText("");
            if ("3".equals(status) || "4".equals(status)) {
                byte[] bytes = extractImageBytes(queried.path("payload").path("result").path("text"));
                if (bytes == null || bytes.length == 0) {
                    return AssetGenerationResult.failure(PROVIDER_NAME, ProviderErrorType.INVALID_RESPONSE,
                            "EMPTY_IMAGE", "HiDream 任务完成但未返回图片");
                }
                return success(bytes, request);
            }
            Thread.sleep(Math.max(200L, image.getHidreamPollIntervalMs()));
        }
        return AssetGenerationResult.failure(PROVIDER_NAME, ProviderErrorType.TIMEOUT,
                "TIMEOUT", "HiDream 图片任务超过等待时间");
    }

    private JsonNode sendJson(HttpClient client, String endpoint, String body, long timeoutMs) throws Exception {
        String authUrl = XfyunSignatureUtil.buildHttpAuthUrl(endpoint, "POST",
                properties.getImageApiKey(), properties.getImageApiSecret());
        IOException lastException = null;
        for (int i = 1; i <= 3; i++) {
            try {
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(authUrl))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofMillis(timeoutMs))
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IllegalStateException("HTTP_" + response.statusCode());
                }
                return MAPPER.readTree(response.body());
            } catch (IOException e) {
                lastException = e;
                log.warn("HiDream HTTP call failed, retry {}/3: {}", i, e.getMessage());
                Thread.sleep(300L * i);
            }
        }
        throw lastException;
    }

    private String queryEndpoint() {
        String configured = properties.getImage().getHidreamQueryEndpoint();
        if (configured != null && !configured.isBlank()) return configured;
        String endpoint = properties.getImage().getEndpoint();
        return endpoint.endsWith("/create") ? endpoint.substring(0, endpoint.length() - 7) + "/query" : endpoint + "/query";
    }

    private byte[] extractImageBytes(JsonNode textNode) throws Exception {
        String encoded = textNode.asText("");
        if (encoded.isBlank()) return null;
        byte[] decoded = decodeBase64Payload(encoded);
        if (isImage(decoded)) return decoded;
        JsonNode result = MAPPER.readTree(decoded);
        String imageUrl = findImageUrl(result);
        if (imageUrl != null) {
            return downloadImage(imageUrl);
        }
        String imageBase64 = findImageBase64(result);
        if (imageBase64 == null || imageBase64.isBlank()) return null;
        return decodeBase64Payload(imageBase64);
    }

    private byte[] decodeBase64Payload(String value) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", "");
        int comma = normalized.indexOf(',');
        if (normalized.startsWith("data:image/") && comma > 0) {
            normalized = normalized.substring(comma + 1);
        }
        try {
            return Base64.getDecoder().decode(normalized);
        } catch (IllegalArgumentException ignored) {
            return Base64.getUrlDecoder().decode(normalized);
        }
    }

    private byte[] downloadImage(String url) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getImage().getTimeoutMs()))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .version(HttpClient.Version.HTTP_1_1)
                .sslContext(SslUtil.createTrustAllContext())
                .build();
        IOException lastException = null;
        for (int i = 1; i <= 3; i++) {
            try {
                HttpResponse<byte[]> response = client.send(HttpRequest.newBuilder(URI.create(url))
                                .timeout(Duration.ofMillis(properties.getImage().getTimeoutMs())).GET().build(),
                        HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() < 200 || response.statusCode() >= 300) return null;
                return response.body();
            } catch (IOException e) {
                lastException = e;
                log.warn("HiDream image download failed, retry {}/3: {}", i, e.getMessage());
                Thread.sleep(300L * i);
            }
        }
        throw lastException;
    }

    private String findImageUrl(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        if (node.isTextual()) {
            String value = node.asText("").trim();
            return isHttpImageUrl(value) ? value : null;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                String value = findImageUrl(child);
                if (value != null) return value;
            }
        }
        if (node.isObject()) {
            for (String field : List.of("image_url", "imageUrl", "origin_image", "originImage",
                    "result_url", "resultUrl", "url", "image", "img_url", "imgUrl")) {
                String value = findImageUrl(node.path(field));
                if (value != null) return value;
            }
            for (var fields = node.fields(); fields.hasNext();) {
                String value = findImageUrl(fields.next().getValue());
                if (value != null) return value;
            }
        }
        return null;
    }

    private String findImageBase64(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        if (node.isTextual()) {
            String value = node.asText("").trim();
            return looksLikeImageBase64(value) ? value : null;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                String value = findImageBase64(child);
                if (value != null) return value;
            }
        }
        if (node.isObject()) {
            Set<String> base64Fields = Set.of("base64", "image_base64", "imageBase64",
                    "img_base64", "imgBase64", "b64_json", "content", "data");
            for (var fields = node.fields(); fields.hasNext();) {
                var entry = fields.next();
                if (base64Fields.contains(entry.getKey())) {
                    String value = findImageBase64(entry.getValue());
                    if (value != null) return value;
                }
            }
            for (var fields = node.fields(); fields.hasNext();) {
                String value = findImageBase64(fields.next().getValue());
                if (value != null) return value;
            }
        }
        return null;
    }

    private boolean isHttpImageUrl(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }

    private boolean looksLikeImageBase64(String value) {
        if (value == null || value.isBlank()) return false;
        if (value.startsWith("data:image/")) return true;
        String normalized = value.trim().replaceAll("\\s+", "");
        if (normalized.length() < 80) return false;
        return normalized.matches("^[A-Za-z0-9+/=_-]+$");
    }

    private AssetGenerationResult success(byte[] bytes, ImageGenerationRequest request) {
        String contentType = detectContentType(bytes);
        String extension = contentType.endsWith("jpeg") ? "jpg" : contentType.substring(contentType.indexOf('/') + 1);
        int actualWidth = request.getWidth() != null ? request.getWidth() : 1024;
        int actualHeight = request.getHeight() != null ? request.getHeight() : 1024;
        try {
            var image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image != null) {
                actualWidth = image.getWidth();
                actualHeight = image.getHeight();
            }
        } catch (Exception ignored) {
            // 部分测试或服务端返回的非标准图片无法读取时，保留请求尺寸。
        }
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("resolution", "2k");
        metadata.put("actualWidth", actualWidth);
        metadata.put("actualHeight", actualHeight);
        metadata.put("assetRole", request.getAssetRole() != null ? request.getAssetRole() : "explanation");
        metadata.put("sizeBytes", bytes.length);
        return AssetGenerationResult.builder().success(true).provider(PROVIDER_NAME)
                .model(properties.getImage().getModel()).content(bytes).contentType(contentType)
                .fileExtension(extension).width(actualWidth).height(actualHeight).metadata(metadata).build();
    }

    private AssetGenerationResult failureForCode(int code, String message) {
        ProviderErrorType type = code == 10021 || code == 10022 || message.contains("审核")
                ? ProviderErrorType.CONTENT_REJECTED : ProviderErrorType.REMOTE_ERROR;
        return AssetGenerationResult.failure(PROVIDER_NAME, type, String.valueOf(code), "HiDream 返回错误");
    }

    private String aspectRatio(ImageGenerationRequest request) {
        int width = request.getWidth() == null ? 1024 : request.getWidth();
        int height = request.getHeight() == null ? 1024 : request.getHeight();
        if (width * 3L == height * 4L) return "4:3";
        if (width * 9L == height * 16L) return "16:9";
        if (height * 9L == width * 16L) return "9:16";
        return "1:1";
    }

    private String buildPrompt(ImageGenerationRequest request) {
        StringBuilder prompt = new StringBuilder();
        if (request.getImageStyle() != null && !request.getImageStyle().isBlank()) {
            prompt.append("教育场景视觉风格：").append(styleDescription(request.getImageStyle())).append("；");
        }
        prompt.append(request.getPrompt());
        prompt.append("；高分辨率，主体清晰，构图稳定，光线均匀，细节丰富，适合大学生学习资源展示");
        if ("cover".equalsIgnoreCase(request.getAssetRole())) {
            prompt.append("；封面构图，主题主体突出，预留干净的标题留白区域");
        } else {
            prompt.append("；知识点示意图，突出结构关系和视觉层次，不在图中生成大段文字");
        }
        return prompt.toString();
    }

    private String styleDescription(String style) {
        return switch (style) {
            case "clean_edu" -> "现代扁平教育插画，白色背景，蓝绿色辅助色";
            case "diagram" -> "清晰的教育信息图和结构化示意图";
            case "blackboard" -> "高对比度黑板手绘教学风格";
            case "isometric" -> "精细等距三维教育插画";
            default -> style;
        };
    }

    private String limit(String value, int max) { return value == null ? "" : value.substring(0, Math.min(max, value.length())); }

    private boolean isImage(byte[] bytes) { return bytes.length > 11 && (((bytes[0] & 255) == 137 && bytes[1] == 80 && bytes[2] == 78 && bytes[3] == 71) || ((bytes[0] & 255) == 255 && (bytes[1] & 255) == 216) || (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P')); }

    private String detectContentType(byte[] bytes) {
        if (bytes.length > 11 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "image/webp";
        }
        return isImage(bytes) && (bytes[0] & 255) == 255 ? "image/jpeg" : "image/png";
    }

    private ProviderErrorType mapException(Exception e) { return e.getMessage() != null && e.getMessage().contains("HTTP_401") ? ProviderErrorType.AUTH_FAILED : ProviderErrorType.REMOTE_ERROR; }
}
