package com.study.ai.multimodal.provider.xfyun;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 科大讯飞鉴权 URL 与签名工具。
 *
 * <p>用于 WebSocket TTS 和 HTTP 文生图请求的鉴权签名生成。
 * 参考：<a href="https://www.xfyun.cn/doc/tts/online_tts/API.html">在线 TTS 文档</a>。
 *
 * <p>安全约束：日志中禁止打印鉴权 URL、签名原文、API Key/Secret。
 */
public final class XfyunSignatureUtil {

    /** HMAC-SHA256 算法名 */
    private static final String HMAC_SHA256 = "HmacSHA256";

    private XfyunSignatureUtil() {
        // 工具类禁止实例化
    }

    /**
     * 生成讯飞 WebSocket TTS 鉴权 URL。
     *
     * <p>签名算法：HMAC-SHA256(apiSecret, "host: {host}\ndate: {date}\nGET {path} HTTP/1.1")，
     * 再将 authorization 进行 Base64 编码拼接到 URL 参数中。
     *
     * @param endpoint  WebSocket 端点，如 wss://tts-api.xfyun.cn/v2/tts
     * @param apiKey    讯飞 APIKey
     * @param apiSecret 讯飞 APISecret
     * @return 拼接好鉴权参数的完整 URL
     * @throws IllegalStateException 签名生成失败
     */
    public static String buildTtsAuthUrl(String endpoint, String apiKey, String apiSecret) {
        try {
            URI uri = URI.create(endpoint);
            String host = uri.getHost();
            String path = uri.getPath();
            if (path == null || path.isEmpty()) {
                path = "/";
            }

            // RFC1123 格式时间
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            String dateStr = format.format(new Date());

            // 签名原文
            String signatureOrigin = "host: " + host + "\n"
                    + "date: " + dateStr + "\n"
                    + "GET " + path + " HTTP/1.1";

            // HMAC-SHA256 签名
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] signatureBytes = mac.doFinal(signatureOrigin.getBytes(StandardCharsets.UTF_8));
            String signatureBase64 = Base64.getEncoder().encodeToString(signatureBytes);

            // authorization 原文
            String authOrigin = "api_key=\"" + apiKey + "\", algorithm=\"hmac-sha256\", "
                    + "headers=\"host date request-line\", signature=\"" + signatureBase64 + "\"";
            String authBase64 = Base64.getEncoder().encodeToString(authOrigin.getBytes(StandardCharsets.UTF_8));

            // 拼接最终鉴权 URL
            return endpoint
                    + "?authorization=" + URLEncoder.encode(authBase64, StandardCharsets.UTF_8)
                    + "&date=" + URLEncoder.encode(dateStr, StandardCharsets.UTF_8)
                    + "&host=" + URLEncoder.encode(host, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Build xfyun auth url failed", e);
        }
    }

    /**
     * 生成讯飞 HTTP API 通用鉴权 URL。
     *
     * <p>签名算法与 WebSocket 鉴权一致，但 request-line 使用实际 HTTP 方法，
     * 例如图片生成接口为 {@code POST /v2.1/tti HTTP/1.1}。讯飞图片生成文档要求
     * authorization/date/host 作为 query 参数拼接到最终请求 URL，而不是 Bearer 头。
     *
     * @param endpoint  HTTP 接口地址
     * @param method    HTTP 方法
     * @param apiKey    讯飞 APIKey
     * @param apiSecret 讯飞 APISecret
     * @return 携带 authorization/date/host query 参数的完整 URL
     */
    public static String buildHttpAuthUrl(String endpoint, String method, String apiKey, String apiSecret) {
        try {
            URI uri = URI.create(endpoint);
            String host = uri.getHost();
            String path = uri.getRawPath();
            if (path == null || path.isEmpty()) {
                path = "/";
            }

            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            String dateStr = format.format(new Date());

            String signatureOrigin = "host: " + host + "\n"
                    + "date: " + dateStr + "\n"
                    + method.toUpperCase(Locale.ROOT) + " " + path + " HTTP/1.1";

            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] signatureBytes = mac.doFinal(signatureOrigin.getBytes(StandardCharsets.UTF_8));
            String signatureBase64 = Base64.getEncoder().encodeToString(signatureBytes);

            String authOrigin = "api_key=\"" + apiKey + "\", algorithm=\"hmac-sha256\", "
                    + "headers=\"host date request-line\", signature=\"" + signatureBase64 + "\"";
            String authBase64 = Base64.getEncoder().encodeToString(authOrigin.getBytes(StandardCharsets.UTF_8));

            String separator = endpoint.contains("?") ? "&" : "?";
            return endpoint
                    + separator
                    + "authorization=" + URLEncoder.encode(authBase64, StandardCharsets.UTF_8)
                    + "&date=" + URLEncoder.encode(dateStr, StandardCharsets.UTF_8)
                    + "&host=" + URLEncoder.encode(host, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Build xfyun http auth url failed", e);
        }
    }
}
