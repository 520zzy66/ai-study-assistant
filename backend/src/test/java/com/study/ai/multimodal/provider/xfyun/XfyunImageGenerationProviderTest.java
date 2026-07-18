package com.study.ai.multimodal.provider.xfyun;

import com.study.ai.multimodal.model.AssetGenerationResult;
import com.study.ai.multimodal.model.ImageGenerationRequest;
import com.study.ai.multimodal.model.ProviderErrorType;
import com.study.config.XfyunProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 讯飞文生图 Provider 单元测试（spec §13.1）。
 *
 * <p>覆盖点：base64 解码、审核失败映射、非法响应处理。
 *
 * <p>实现说明：通过 {@link MockedStatic} 拦截 {@link HttpClient#newBuilder()}，
 * 拦截 {@link HttpClient#send} 返回构造的响应，避免真实网络调用。
 * 每个测试方法通过 {@link #stubHttp(int, String)} 配置状态码与响应体。
 */
class XfyunImageGenerationProviderTest {

    private XfyunProperties properties;
    private XfyunImageGenerationProvider provider;

    private MockedStatic<HttpClient> mockedHttpClient;
    private HttpClient mockClient;
    private HttpClient.Builder mockBuilder;
    private HttpResponse<String> mockResponse;

    @BeforeEach
    void setUp() {
        properties = new XfyunProperties();
        properties.setAppId("test-appid");
        properties.setApiKey("test-apikey");
        properties.setApiSecret("test-apisecret");
        properties.getImage().setEnabled(true);
        properties.getImage().setProvider("tti");
        properties.getImage().setEndpoint("https://spark-api.cn-huabei-1.xf-yun.com/v2.1/tti");
        properties.getImage().setModel("spark-tti");
        properties.getImage().setTimeoutMs(3000L);
        properties.getImage().setDefaultSize("512x512");
        provider = new XfyunImageGenerationProvider(properties);

        // 打开 MockedStatic，所有测试方法均可使用
        mockedHttpClient = mockStatic(HttpClient.class);
        mockClient = mock(HttpClient.class);
        mockBuilder = mock(HttpClient.Builder.class);
        mockResponse = mock(HttpResponse.class);

        mockedHttpClient.when(HttpClient::newBuilder).thenReturn(mockBuilder);
        when(mockBuilder.connectTimeout(any())).thenReturn(mockBuilder);
        // 兼容主代码 .sslContext(SslUtil.createTrustAllContext()) 链
        when(mockBuilder.sslContext(any())).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockClient);
    }

    @AfterEach
    void tearDown() {
        if (mockedHttpClient != null) {
            mockedHttpClient.close();
        }
    }

    @Test
    void generate_ConfigMissing_ReturnsConfigMissing() {
        properties.getImage().setEnabled(false);

        ImageGenerationRequest request = new ImageGenerationRequest();
        request.setPrompt("测试图片");

        List<AssetGenerationResult> results = provider.generate(request);

        assertEquals(1, results.size());
        AssetGenerationResult result = results.get(0);
        assertFalse(result.isSuccess());
        assertEquals(ProviderErrorType.CONFIG_MISSING, result.getErrorType());
        assertEquals("CONFIG_MISSING", result.getErrorCode());
    }

    @Test
    void generate_EmptyPrompt_ReturnsInvalidResponse() {
        ImageGenerationRequest request = new ImageGenerationRequest();
        request.setPrompt("   ");

        List<AssetGenerationResult> results = provider.generate(request);

        assertEquals(1, results.size());
        AssetGenerationResult result = results.get(0);
        assertFalse(result.isSuccess());
        assertEquals(ProviderErrorType.INVALID_RESPONSE, result.getErrorType());
        assertEquals("EMPTY_PROMPT", result.getErrorCode());
    }

    @Test
    void generate_Success_PngBase64_DecodedToBytes() throws Exception {
        // 构造 PNG 文件头字节
        byte[] pngBytes = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x10, 0x20, 0x30, 0x40
        };
        String base64Image = Base64.getEncoder().encodeToString(pngBytes);
        String responseBody = "{"
                + "\"header\":{\"code\":0},"
                + "\"payload\":{\"choices\":{\"text\":[{\"content\":\"" + base64Image + "\"}]}}"
                + "}";

        stubHttp(200, responseBody);

        ImageGenerationRequest request = new ImageGenerationRequest();
        request.setPrompt("知识图谱");
        request.setImageStyle("clean_edu");
        request.setWidth(512);
        request.setHeight(512);
        request.setAssetRole("cover");

        List<AssetGenerationResult> results = provider.generate(request);

        assertEquals(1, results.size());
        AssetGenerationResult result = results.get(0);
        assertTrue(result.isSuccess(), "应成功生成图片");
        assertEquals("xfyun-tti", result.getProvider());
        assertEquals("image/png", result.getContentType());
        assertEquals("png", result.getFileExtension());
        assertEquals(512, result.getWidth());
        assertEquals(512, result.getHeight());
        assertNotNull(result.getContent());
        assertArrayEquals(pngBytes, result.getContent());
        assertNotNull(result.getMetadata());
        assertEquals(512, result.getMetadata().get("width"));
        assertEquals("cover", result.getMetadata().get("assetRole"));
    }

    @Test
    void generate_Success_JpegBase64_DetectsJpegContentType() throws Exception {
        // JPEG 文件头：FF D8 FF
        byte[] jpegBytes = new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x10, 0x20, 0x30, 0x40
        };
        String base64Image = Base64.getEncoder().encodeToString(jpegBytes);
        String responseBody = "{"
                + "\"header\":{\"code\":0},"
                + "\"payload\":{\"choices\":{\"text\":[{\"content\":\"" + base64Image + "\"}]}}"
                + "}";

        stubHttp(200, responseBody);

        ImageGenerationRequest request = new ImageGenerationRequest();
        request.setPrompt("测试 JPEG");

        List<AssetGenerationResult> results = provider.generate(request);

        AssetGenerationResult result = results.get(0);
        assertTrue(result.isSuccess());
        assertEquals("image/jpeg", result.getContentType());
        assertEquals("jpg", result.getFileExtension());
        assertArrayEquals(jpegBytes, result.getContent());
    }

    @Test
    void generate_InvalidBase64_ReturnsInvalidResponse() throws Exception {
        // 非法 base64（包含 ! 等非法字符）
        String responseBody = "{"
                + "\"header\":{\"code\":0},"
                + "\"payload\":{\"choices\":{\"text\":[{\"content\":\"!!!not-base64!!!\"}]}}"
                + "}";

        stubHttp(200, responseBody);

        ImageGenerationRequest request = new ImageGenerationRequest();
        request.setPrompt("测试非法 base64");

        List<AssetGenerationResult> results = provider.generate(request);

        AssetGenerationResult result = results.get(0);
        assertFalse(result.isSuccess());
        assertEquals(ProviderErrorType.INVALID_RESPONSE, result.getErrorType());
        assertEquals("INVALID_BASE64", result.getErrorCode());
    }

    @Test
    void generate_EmptyImageContent_ReturnsEmptyImage() throws Exception {
        // text 数组存在但 content 为空
        String responseBody = "{"
                + "\"header\":{\"code\":0},"
                + "\"payload\":{\"choices\":{\"text\":[{\"content\":\"\"}]}}"
                + "}";

        stubHttp(200, responseBody);

        ImageGenerationRequest request = new ImageGenerationRequest();
        request.setPrompt("测试空图片");

        List<AssetGenerationResult> results = provider.generate(request);

        AssetGenerationResult result = results.get(0);
        assertFalse(result.isSuccess());
        assertEquals(ProviderErrorType.INVALID_RESPONSE, result.getErrorType());
        assertEquals("EMPTY_IMAGE", result.getErrorCode());
    }

    @Test
    void generate_EmptyTextArray_ReturnsEmptyImage() throws Exception {
        // payload.choices.text 不是数组或为空数组
        String responseBody = "{"
                + "\"header\":{\"code\":0},"
                + "\"payload\":{\"choices\":{\"text\":[]}}"
                + "}";

        stubHttp(200, responseBody);

        ImageGenerationRequest request = new ImageGenerationRequest();
        request.setPrompt("测试空数组");

        List<AssetGenerationResult> results = provider.generate(request);

        AssetGenerationResult result = results.get(0);
        assertFalse(result.isSuccess());
        assertEquals(ProviderErrorType.INVALID_RESPONSE, result.getErrorType());
        assertEquals("EMPTY_IMAGE", result.getErrorCode());
    }

    @Test
    void generate_ResponseCode10043_MapsToContentRejected() throws Exception {
        // 10043 是讯飞内容审核错误码
        String responseBody = "{"
                + "\"header\":{\"code\":10043,\"message\":\"content moderation failed\"}"
                + "}";

        stubHttp(200, responseBody);

        ImageGenerationRequest request = new ImageGenerationRequest();
        request.setPrompt("敏感内容");

        List<AssetGenerationResult> results = provider.generate(request);

        AssetGenerationResult result = results.get(0);
        assertFalse(result.isSuccess());
        assertEquals(ProviderErrorType.CONTENT_REJECTED, result.getErrorType(),
                "10043 应映射为 CONTENT_REJECTED");
        assertEquals("10043", result.getErrorCode());
    }

    @Test
    void generate_ResponseCode10040_MapsToContentRejected() throws Exception {
        // 10040 也是讯飞审核相关错误码
        String responseBody = "{"
                + "\"header\":{\"code\":10040,\"message\":\"sensitive content\"}"
                + "}";

        stubHttp(200, responseBody);

        ImageGenerationRequest request = new ImageGenerationRequest();
        request.setPrompt("敏感内容");

        List<AssetGenerationResult> results = provider.generate(request);

        AssetGenerationResult result = results.get(0);
        assertFalse(result.isSuccess());
        assertEquals(ProviderErrorType.CONTENT_REJECTED, result.getErrorType());
    }

    @Test
    void generate_ResponseMessageContainsAudit_MapsToContentRejected() throws Exception {
        // 非 10043/10040 但 message 包含 "审核"
        String responseBody = "{"
                + "\"header\":{\"code\":10099,\"message\":\"内容审核未通过\"}"
                + "}";

        stubHttp(200, responseBody);

        ImageGenerationRequest request = new ImageGenerationRequest();
        request.setPrompt("测试审核关键字");

        List<AssetGenerationResult> results = provider.generate(request);

        AssetGenerationResult result = results.get(0);
        assertFalse(result.isSuccess());
        assertEquals(ProviderErrorType.CONTENT_REJECTED, result.getErrorType(),
                "message 包含 '审核' 应映射为 CONTENT_REJECTED");
    }

    @Test
    void generate_Http401_MapsToAuthFailed() throws Exception {
        stubHttp(401, "{\"header\":{\"code\":401,\"message\":\"unauthorized\"}}");

        ImageGenerationRequest request = new ImageGenerationRequest();
        request.setPrompt("测试 401");

        List<AssetGenerationResult> results = provider.generate(request);

        AssetGenerationResult result = results.get(0);
        assertFalse(result.isSuccess());
        assertEquals(ProviderErrorType.AUTH_FAILED, result.getErrorType());
        assertEquals("HTTP_401", result.getErrorCode());
    }

    @Test
    void generate_Http429_MapsToRateLimited() throws Exception {
        stubHttp(429, "{}");

        ImageGenerationRequest request = new ImageGenerationRequest();
        request.setPrompt("测试限流");

        List<AssetGenerationResult> results = provider.generate(request);

        AssetGenerationResult result = results.get(0);
        assertFalse(result.isSuccess());
        assertEquals(ProviderErrorType.RATE_LIMITED, result.getErrorType());
        assertEquals("HTTP_429", result.getErrorCode());
    }

    @Test
    void generate_Http400_BodyContainsSensitive_MapsToContentRejected() throws Exception {
        // HTTP 400 + 响应体包含 "sensitive" 关键字 → CONTENT_REJECTED
        stubHttp(400, "{\"error\":\"sensitive content detected\"}");

        ImageGenerationRequest request = new ImageGenerationRequest();
        request.setPrompt("敏感内容");

        List<AssetGenerationResult> results = provider.generate(request);

        AssetGenerationResult result = results.get(0);
        assertFalse(result.isSuccess());
        assertEquals(ProviderErrorType.CONTENT_REJECTED, result.getErrorType());
    }

    @Test
    void generate_Http500_MapsToRemoteError() throws Exception {
        stubHttp(500, "internal server error");

        ImageGenerationRequest request = new ImageGenerationRequest();
        request.setPrompt("测试 500");

        List<AssetGenerationResult> results = provider.generate(request);

        AssetGenerationResult result = results.get(0);
        assertFalse(result.isSuccess());
        assertEquals(ProviderErrorType.REMOTE_ERROR, result.getErrorType());
        assertEquals("HTTP_500", result.getErrorCode());
    }

    @Test
    void generate_HttpRequestContainsSignedQueryAndJsonContentType() throws Exception {
        byte[] pngBytes = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        };
        String base64Image = Base64.getEncoder().encodeToString(pngBytes);
        String responseBody = "{"
                + "\"header\":{\"code\":0},"
                + "\"payload\":{\"choices\":{\"text\":[{\"content\":\"" + base64Image + "\"}]}}"
                + "}";

        stubHttp(200, responseBody);

        ImageGenerationRequest request = new ImageGenerationRequest();
        request.setPrompt("测试请求头");
        request.setImageStyle("diagram");
        request.setNegativePrompt("模糊");

        provider.generate(request);

        // 验证 HttpRequest 使用讯飞 HTTP 通用鉴权 query 参数与 JSON Content-Type
        verify(mockClient).send(argThat(req -> {
            String uri = req.uri().toString();
            String ct = req.headers().firstValue("Content-Type").orElse("");
            return uri.contains("authorization=")
                    && uri.contains("date=")
                    && uri.contains("host=")
                    && req.headers().firstValue("Authorization").isEmpty()
                    && ct.startsWith("application/json");
        }), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void isAvailable_Configured_ReturnsTrue() {
        assertTrue(provider.isAvailable());
    }

    @Test
    void isAvailable_NotConfigured_ReturnsFalse() {
        properties.getImage().setEnabled(false);
        assertFalse(provider.isAvailable());
    }

    /**
     * 配置 mockClient.send 返回固定的 HTTP 响应。
     *
     * @param statusCode HTTP 状态码
     * @param body       响应体
     */
    @SuppressWarnings("unchecked")
    private void stubHttp(int statusCode, String body) {
        when(mockResponse.statusCode()).thenReturn(statusCode);
        when(mockResponse.body()).thenReturn(body);
        try {
            when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(mockResponse);
        } catch (Exception e) {
            // send 声明 throws，但 mock 不会真的抛
            fail("stubbing send should not throw", e);
        }
    }
}
