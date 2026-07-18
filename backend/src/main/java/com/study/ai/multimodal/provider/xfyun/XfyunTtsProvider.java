package com.study.ai.multimodal.provider.xfyun;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.ai.multimodal.model.AssetGenerationResult;
import com.study.ai.multimodal.model.AudioSynthesisRequest;
import com.study.ai.multimodal.model.ProviderErrorType;
import com.study.ai.multimodal.provider.SpeechSynthesisProvider;
import com.study.config.XfyunProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 科大讯飞在线语音合成 WebSocket Provider 实现。
 *
 * <p>实现细节：
 * <ul>
 *   <li>使用 JDK 内置 {@link java.net.http.WebSocket}，无需额外依赖。</li>
 *   <li>鉴权 URL 通过 {@link XfyunSignatureUtil} 生成。</li>
 *   <li>请求帧：common.appid + business(aue=lame, sfl=1) + data(text Base64)。</li>
 *   <li>响应帧：data.audio(Base64) 累积拼接；data.status=2 表示合成结束。</li>
 *   <li>错误码非 0 时映射为对应 {@link ProviderErrorType}。</li>
 * </ul>
 *
 * <p>安全约束：日志中不打印鉴权 URL、签名原文、文本内容和音频字节。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class XfyunTtsProvider implements SpeechSynthesisProvider {

    private static final String PROVIDER_NAME = "xfyun-tts";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final XfyunProperties xfyunProperties;

    @Override
    public AssetGenerationResult synthesize(AudioSynthesisRequest request) {
        if (!isAvailable()) {
            return AssetGenerationResult.failure(PROVIDER_NAME, ProviderErrorType.CONFIG_MISSING,
                    "CONFIG_MISSING", "讯飞 TTS 未配置或未启用");
        }
        String text = request.getText();
        if (text == null || text.isBlank()) {
            return AssetGenerationResult.failure(PROVIDER_NAME, ProviderErrorType.INVALID_RESPONSE,
                    "EMPTY_TEXT", "待合成文本为空");
        }
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        if (textBytes.length > xfyunProperties.getTts().getMaxTextBytes()) {
            return AssetGenerationResult.failure(PROVIDER_NAME, ProviderErrorType.TEXT_TOO_LONG,
                    "TEXT_TOO_LONG", "播客脚本超过单次合成上限");
        }
        String voice = (request.getVoice() == null || request.getVoice().isBlank())
                ? xfyunProperties.getTts().getVoice()
                : request.getVoice();
        try {
            return doSynthesize(text, voice);
        } catch (Exception e) {
            log.warn("Xfyun TTS synthesize failed: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return AssetGenerationResult.failure(PROVIDER_NAME, mapException(e),
                    "REMOTE_ERROR", "讯飞 TTS 调用失败: " + e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        return xfyunProperties.isTtsConfigured();
    }

    /**
     * 执行 WebSocket 调用并拼接 MP3 字节。
     */
    private AssetGenerationResult doSynthesize(String text, String voice) throws Exception {
        String authUrl = XfyunSignatureUtil.buildTtsAuthUrl(
                xfyunProperties.getTts().getEndpoint(),
                xfyunProperties.getTtsApiKey(),
                xfyunProperties.getTtsApiSecret());

        // 构造请求帧
        Map<String, Object> frame = buildRequestFrame(text, voice);
        String frameJson = MAPPER.writeValueAsString(frame);

        CompletableFuture<byte[]> future = new CompletableFuture<>();
        ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();
        StringBuilder messageBuffer = new StringBuilder();
        AtomicReference<Integer> errorCode = new AtomicReference<>();
        AtomicReference<String> errorMessage = new AtomicReference<>();

        HttpClient client = HttpClient.newBuilder()
                .sslContext(SslUtil.createTrustAllContext())
                .build();
        long timeoutMs = xfyunProperties.getTts().getTimeoutMs();

        WebSocket.Listener listener = new WebSocket.Listener() {
            @Override
            public void onOpen(WebSocket webSocket) {
                webSocket.sendText(frameJson, true);
                webSocket.request(1);
            }

            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                // 累积分片：讯飞大音频 base64 响应可能被 WebSocket 协议拆分成多帧，
                // 必须等 last=true 才能拿到完整 JSON，否则 Jackson 会抛 JsonEOFException
                messageBuffer.append(data);
                if (!last) {
                    webSocket.request(1);
                    return CompletableFuture.completedFuture(null);
                }
                String fullText = messageBuffer.toString();
                messageBuffer.setLength(0);
                try {
                    JsonNode node = MAPPER.readTree(fullText);
                    int code = node.path("code").asInt(-1);
                    if (code != 0) {
                        errorCode.set(code);
                        errorMessage.set(node.path("message").asText("unknown"));
                        future.completeExceptionally(new IllegalStateException("code=" + code));
                        return CompletableFuture.completedFuture(null);
                    }
                    JsonNode dataNode = node.path("data");
                    if (!dataNode.isMissingNode()) {
                        String audio = dataNode.path("audio").asText("");
                        if (!audio.isEmpty()) {
                            // 每帧独立解码后累积字节（讯飞每帧 audio 是独立完整的 base64，带 padding）
                            byte[] chunk = Base64.getDecoder().decode(audio);
                            audioBuffer.write(chunk);
                        }
                        int status = dataNode.path("status").asInt(0);
                        if (status == 2) {
                            future.complete(audioBuffer.toByteArray());
                            return CompletableFuture.completedFuture(null);
                        }
                    }
                } catch (Exception e) {
                    future.completeExceptionally(e);
                    return CompletableFuture.completedFuture(null);
                }
                webSocket.request(1);
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public void onError(WebSocket webSocket, Throwable error) {
                future.completeExceptionally(error);
            }
        };

        WebSocket webSocket = client.newWebSocketBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .buildAsync(URI.create(authUrl), listener)
                .get(timeoutMs, TimeUnit.MILLISECONDS);

        byte[] mp3Bytes;
        try {
            mp3Bytes = future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } finally {
            try {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "ok");
            } catch (Exception ignored) {
                // 关闭失败不影响结果
            }
        }

        if (mp3Bytes == null || mp3Bytes.length == 0) {
            return AssetGenerationResult.failure(PROVIDER_NAME, ProviderErrorType.INVALID_RESPONSE,
                    "EMPTY_AUDIO", "合成音频为空");
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("voice", voice);
        metadata.put("sizeBytes", mp3Bytes.length);

        return AssetGenerationResult.builder()
                .success(true)
                .provider(PROVIDER_NAME)
                .model(voice)
                .content(mp3Bytes)
                .contentType("audio/mpeg")
                .fileExtension("mp3")
                .metadata(metadata)
                .build();
    }

    /**
     * 构造讯飞 TTS 请求帧。
     */
    private Map<String, Object> buildRequestFrame(String text, String voice) {
        Map<String, Object> frame = new HashMap<>();
        Map<String, Object> common = new HashMap<>();
        common.put("app_id", xfyunProperties.getTtsAppId());
        frame.put("common", common);

        Map<String, Object> business = new HashMap<>();
        business.put("aue", "lame");
        business.put("sfl", 1);
        // 讯飞 TTS v2 必传参数名为 vcn（发音人），不是 voice，否则返回 10163 缺少参数
        business.put("vcn", voice);
        business.put("speed", 50);
        business.put("volume", 50);
        business.put("pitch", 50);
        // tte 不传：默认按 UTF-8 编码解析 text。
        // 若设为 unicode2utf8，讯飞会按 \\uXXXX 转义序列解析，但当前传的是普通 UTF-8 文本，
        // 解析失败会合成出乱码音频（听不懂、不按稿子）。
        business.put("tte", "UTF8");
        frame.put("business", business);

        Map<String, Object> data = new HashMap<>();
        data.put("status", 2);
        data.put("text", Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8)));
        frame.put("data", data);

        return frame;
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
