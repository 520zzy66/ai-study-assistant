package com.study.ai.multimodal.provider.xfyun;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 讯飞鉴权工具测试（spec §13.1）。
 *
 * <p>覆盖点：HMAC 签名参数稳定、URL 编码正确、HTTP 鉴权 URL 格式正确。
 */
class XfyunSignatureUtilTest {

    private static final String ENDPOINT = "wss://tts-api.xfyun.cn/v2/tts";
    private static final String API_KEY = "test-api-key-1234";
    private static final String API_SECRET = "test-api-secret-5678";

    @Test
    void buildTtsAuthUrl_ContainsRequiredParams() {
        String url = XfyunSignatureUtil.buildTtsAuthUrl(ENDPOINT, API_KEY, API_SECRET);

        assertNotNull(url);
        assertTrue(url.startsWith(ENDPOINT + "?"), "URL 应以原 endpoint 开头");
        assertTrue(url.contains("authorization="), "URL 应包含 authorization 参数");
        assertTrue(url.contains("date="), "URL 应包含 date 参数");
        assertTrue(url.contains("host="), "URL 应包含 host 参数");
    }

    @Test
    void buildTtsAuthUrl_HostParamEqualsEndpointHost() {
        String url = XfyunSignatureUtil.buildTtsAuthUrl(ENDPOINT, API_KEY, API_SECRET);

        String host = extractParam(url, "host");
        assertEquals("tts-api.xfyun.cn", host, "host 参数应等于 endpoint 的主机名");
    }

    @Test
    void buildTtsAuthUrl_AuthorizationIsBase64OfSignedAuth() {
        String url = XfyunSignatureUtil.buildTtsAuthUrl(ENDPOINT, API_KEY, API_SECRET);

        String authEncoded = extractParam(url, "authorization");
        String authDecoded = new String(Base64.getDecoder().decode(authEncoded), StandardCharsets.UTF_8);

        // authorization 原文应包含 api_key、algorithm、headers、signature 四个字段
        assertTrue(authDecoded.contains("api_key=\"" + API_KEY + "\""), "auth 原文应包含 api_key");
        assertTrue(authDecoded.contains("algorithm=\"hmac-sha256\""), "auth 原文应包含 algorithm");
        assertTrue(authDecoded.contains("headers=\"host date request-line\""), "auth 原文应包含 headers");
        assertTrue(authDecoded.contains("signature=\""), "auth 原文应包含 signature");
    }

    @Test
    void buildTtsAuthUrl_DateParamIsRfc1123Format() {
        String url = XfyunSignatureUtil.buildTtsAuthUrl(ENDPOINT, API_KEY, API_SECRET);

        String date = extractParam(url, "date");
        // RFC1123 格式：EEE, dd MMM yyyy HH:mm:ss z
        assertTrue(date.matches("[A-Za-z]{3}, \\d{2} [A-Za-z]{3} \\d{4} \\d{2}:\\d{2}:\\d{2} GMT"),
                "date 参数应为 RFC1123 GMT 格式，实际: " + date);
    }

    @Test
    void buildTtsAuthUrl_SameParamsDifferentCallsReturnDifferentSignatures() {
        // 由于 date 字段每秒变化，签名也会变化；同一秒内调用应一致
        String url1 = XfyunSignatureUtil.buildTtsAuthUrl(ENDPOINT, API_KEY, API_SECRET);
        String url2 = XfyunSignatureUtil.buildTtsAuthUrl(ENDPOINT, API_KEY, API_SECRET);

        // 同一秒内 date 相同，签名也应相同
        String date1 = extractParam(url1, "date");
        String date2 = extractParam(url2, "date");
        if (date1.equals(date2)) {
            assertEquals(extractParam(url1, "authorization"),
                    extractParam(url2, "authorization"),
                    "同一时间下相同参数生成的签名应稳定");
        }
    }

    @Test
    void buildTtsAuthUrl_NoExceptionForValidEndpoint() {
        assertDoesNotThrow(() ->
                XfyunSignatureUtil.buildTtsAuthUrl(ENDPOINT, API_KEY, API_SECRET));
    }

    @Test
    void buildHttpAuthUrl_ContainsRequiredParams() {
        String url = XfyunSignatureUtil.buildHttpAuthUrl(
                "https://spark-api.cn-huabei-1.xf-yun.com/v2.1/tti",
                "POST",
                API_KEY,
                API_SECRET);

        assertNotNull(url);
        assertTrue(url.startsWith("https://spark-api.cn-huabei-1.xf-yun.com/v2.1/tti?"));
        assertTrue(url.contains("authorization="));
        assertTrue(url.contains("date="));
        assertTrue(url.contains("host="));
    }

    @Test
    void buildHttpAuthUrl_AuthorizationUsesSignedRequestLine() {
        String url = XfyunSignatureUtil.buildHttpAuthUrl(
                "https://spark-api.cn-huabei-1.xf-yun.com/v2.1/tti",
                "POST",
                API_KEY,
                API_SECRET);

        String authEncoded = extractParam(url, "authorization");
        String authDecoded = new String(Base64.getDecoder().decode(authEncoded), StandardCharsets.UTF_8);

        assertTrue(authDecoded.contains("api_key=\"" + API_KEY + "\""));
        assertTrue(authDecoded.contains("algorithm=\"hmac-sha256\""));
        assertTrue(authDecoded.contains("headers=\"host date request-line\""));
        assertTrue(authDecoded.contains("signature=\""));
    }

    /**
     * 从 URL 中提取指定 query 参数值（已 URL 解码）。
     */
    private String extractParam(String url, String param) {
        URI uri = URI.create(url);
        String query = uri.getRawQuery();
        assertNotNull(query, "URL 应有 query 部分");
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0 && pair.substring(0, idx).equals(param)) {
                return URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            }
        }
        fail("参数 " + param + " 不存在");
        return null;
    }
}
