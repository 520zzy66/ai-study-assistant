package com.study.ai.multimodal.provider.xfyun;

import com.study.ai.multimodal.model.AssetGenerationResult;
import com.study.ai.multimodal.model.AudioSynthesisRequest;
import com.study.ai.multimodal.model.ProviderErrorType;
import com.study.config.XfyunProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 讯飞 TTS Provider 单元测试（spec §13.1）。
 *
 * <p>覆盖点：成功响应拼接、结束帧识别、错误码映射。
 *
 * <p>实现说明：通过 {@link MockedStatic} 拦截 {@link HttpClient#newBuilder()}，
 * 捕获 {@link WebSocket.Listener} 并手动触发回调，避免真实网络调用。
 */
class XfyunTtsProviderTest {

    private XfyunProperties properties;
    private XfyunTtsProvider provider;

    @BeforeEach
    void setUp() {
        properties = new XfyunProperties();
        properties.setAppId("test-appid");
        properties.setApiKey("test-apikey");
        properties.setApiSecret("test-apisecret");
        properties.getTts().setEnabled(true);
        properties.getTts().setEndpoint("wss://tts-api.xfyun.cn/v2/tts");
        properties.getTts().setVoice("xiaoyan");
        properties.getTts().setTimeoutMs(3000L);
        properties.getTts().setMaxTextBytes(1024);
        provider = new XfyunTtsProvider(properties);
    }

    /**
     * 在 MockedStatic 作用域内拦截 HttpClient.newBuilder() 链式调用，
     * 返回 mock HttpClient，并附带已 mock 的 WebSocket.Builder。
     */
    private HttpClient mockHttpClientWithWebSocket(MockedStatic<HttpClient> mockedHttpClient,
                                                    WebSocket.Builder mockWsBuilder) {
        HttpClient mockClient = mock(HttpClient.class);
        HttpClient.Builder mockHttpBuilder = mock(HttpClient.Builder.class);
        mockedHttpClient.when(HttpClient::newBuilder).thenReturn(mockHttpBuilder);
        when(mockHttpBuilder.sslContext(any())).thenReturn(mockHttpBuilder);
        when(mockHttpBuilder.build()).thenReturn(mockClient);
        when(mockClient.newWebSocketBuilder()).thenReturn(mockWsBuilder);
        when(mockWsBuilder.connectTimeout(any())).thenReturn(mockWsBuilder);
        return mockClient;
    }

    @Test
    void synthesize_ConfigMissing_ReturnsConfigMissingFailure() {
        // 关闭 TTS enabled
        properties.getTts().setEnabled(false);

        AudioSynthesisRequest request = new AudioSynthesisRequest();
        request.setText("测试文本");

        AssetGenerationResult result = provider.synthesize(request);

        assertFalse(result.isSuccess());
        assertEquals(ProviderErrorType.CONFIG_MISSING, result.getErrorType());
        assertEquals("CONFIG_MISSING", result.getErrorCode());
    }

    @Test
    void synthesize_EmptyText_ReturnsInvalidResponse() {
        AudioSynthesisRequest request = new AudioSynthesisRequest();
        request.setText("");

        AssetGenerationResult result = provider.synthesize(request);

        assertFalse(result.isSuccess());
        assertEquals(ProviderErrorType.INVALID_RESPONSE, result.getErrorType());
        assertEquals("EMPTY_TEXT", result.getErrorCode());
    }

    @Test
    void synthesize_TextTooLong_ReturnsTextTooLong() {
        AudioSynthesisRequest request = new AudioSynthesisRequest();
        // 构造超过 maxTextBytes 的文本
        String longText = "a".repeat(2000);
        request.setText(longText);

        AssetGenerationResult result = provider.synthesize(request);

        assertFalse(result.isSuccess());
        assertEquals(ProviderErrorType.TEXT_TOO_LONG, result.getErrorType());
        assertEquals("TEXT_TOO_LONG", result.getErrorCode());
    }

    @Test
    void synthesize_Success_AccumulatesAudioAndCompletesOnStatus2() throws Exception {
        // 构造合法 MP3 字节（带 ID3 文件头）
        byte[] mp3Bytes = new byte[]{0x49, 0x44, 0x33, 0x10, 0x20, 0x30, 0x40};
        String audioBase64 = Base64.getEncoder().encodeToString(mp3Bytes);

        try (var mockedHttpClient = mockStatic(HttpClient.class)) {
            WebSocket.Builder mockBuilder = mock(WebSocket.Builder.class);
            WebSocket mockSocket = mock(WebSocket.class);
            mockHttpClientWithWebSocket(mockedHttpClient, mockBuilder);

            ArgumentCaptor<WebSocket.Listener> listenerCaptor = ArgumentCaptor.forClass(WebSocket.Listener.class);
            when(mockBuilder.buildAsync(any(), listenerCaptor.capture()))
                    .thenAnswer(invocation -> {
                        WebSocket.Listener listener = invocation.getArgument(1);
                        // 在独立线程触发回调，避免阻塞 buildAsync 调用本身
                        CompletableFuture.runAsync(() -> {
                            listener.onOpen(mockSocket);
                            // 第 1 帧：音频数据，status=1（未结束）
                            String frame1 = "{\"code\":0,\"data\":{\"audio\":\"" + audioBase64 + "\",\"status\":1}}";
                            listener.onText(mockSocket, frame1, true);
                            // 第 2 帧：空音频 + status=2（结束）
                            String frame2 = "{\"code\":0,\"data\":{\"audio\":\"\",\"status\":2}}";
                            listener.onText(mockSocket, frame2, true);
                        });
                        return CompletableFuture.completedFuture(mockSocket);
                    });

            AudioSynthesisRequest request = new AudioSynthesisRequest();
            request.setText("测试文本");
            request.setVoice("xiaoyan");

            AssetGenerationResult result = provider.synthesize(request);

            assertTrue(result.isSuccess(), "合成应成功");
            assertEquals("audio/mpeg", result.getContentType());
            assertEquals("mp3", result.getFileExtension());
            assertEquals("xfyun-tts", result.getProvider());
            assertNotNull(result.getContent());
            assertEquals(mp3Bytes.length, result.getContent().length);
            // 验证音频字节按 Base64 解码后与原始字节一致
            assertArrayEquals(mp3Bytes, result.getContent());

            // 验证 listener 被正确捕获
            assertNotNull(listenerCaptor.getValue());
            // 验证 WebSocket 被关闭
            verify(mockSocket, atLeastOnce()).sendClose(eq(WebSocket.NORMAL_CLOSURE), anyString());
        }
    }

    @Test
    void synthesize_MultipleAudioFrames_AreAccumulatedBeforeDecode() throws Exception {
        // 拆分为两段 Base64，验证每帧独立解码后再累积字节的逻辑
        byte[] part1 = new byte[]{0x49, 0x44, 0x33, 0x10};
        byte[] part2 = new byte[]{0x20, 0x30, 0x40, 0x50};
        String base64Part1 = Base64.getEncoder().encodeToString(part1);
        String base64Part2 = Base64.getEncoder().encodeToString(part2);
        byte[] expected = new byte[part1.length + part2.length];
        System.arraycopy(part1, 0, expected, 0, part1.length);
        System.arraycopy(part2, 0, expected, part1.length, part2.length);

        try (var mockedHttpClient = mockStatic(HttpClient.class)) {
            WebSocket.Builder mockBuilder = mock(WebSocket.Builder.class);
            WebSocket mockSocket = mock(WebSocket.class);
            mockHttpClientWithWebSocket(mockedHttpClient, mockBuilder);

            ArgumentCaptor<WebSocket.Listener> listenerCaptor = ArgumentCaptor.forClass(WebSocket.Listener.class);
            when(mockBuilder.buildAsync(any(), listenerCaptor.capture()))
                    .thenAnswer(invocation -> {
                        WebSocket.Listener listener = invocation.getArgument(1);
                        CompletableFuture.runAsync(() -> {
                            listener.onOpen(mockSocket);
                            listener.onText(mockSocket,
                                    "{\"code\":0,\"data\":{\"audio\":\"" + base64Part1 + "\",\"status\":1}}", true);
                            listener.onText(mockSocket,
                                    "{\"code\":0,\"data\":{\"audio\":\"" + base64Part2 + "\",\"status\":1}}", true);
                            listener.onText(mockSocket,
                                    "{\"code\":0,\"data\":{\"audio\":\"\",\"status\":2}}", true);
                        });
                        return CompletableFuture.completedFuture(mockSocket);
                    });

            AudioSynthesisRequest request = new AudioSynthesisRequest();
            request.setText("测试多帧");

            AssetGenerationResult result = provider.synthesize(request);

            assertTrue(result.isSuccess());
            assertArrayEquals(expected, result.getContent(), "多帧 Base64 应各自解码后再累积字节");
        }
    }

    @Test
    void synthesize_ServerReturnsNonZeroCode_MapsToFailure() throws Exception {
        try (var mockedHttpClient = mockStatic(HttpClient.class)) {
            WebSocket.Builder mockBuilder = mock(WebSocket.Builder.class);
            WebSocket mockSocket = mock(WebSocket.class);
            mockHttpClientWithWebSocket(mockedHttpClient, mockBuilder);

            ArgumentCaptor<WebSocket.Listener> listenerCaptor = ArgumentCaptor.forClass(WebSocket.Listener.class);
            when(mockBuilder.buildAsync(any(), listenerCaptor.capture()))
                    .thenAnswer(invocation -> {
                        WebSocket.Listener listener = invocation.getArgument(1);
                        CompletableFuture.runAsync(() -> {
                            listener.onOpen(mockSocket);
                            // 讯飞错误码 10043：内容审核失败
                            listener.onText(mockSocket,
                                    "{\"code\":10043,\"message\":\"content moderation failed\"}", true);
                        });
                        return CompletableFuture.completedFuture(mockSocket);
                    });

            AudioSynthesisRequest request = new AudioSynthesisRequest();
            request.setText("测试错误码");

            AssetGenerationResult result = provider.synthesize(request);

            assertFalse(result.isSuccess(), "服务端返回错误码时应失败");
            assertEquals("REMOTE_ERROR", result.getErrorCode());
            // errorMessage 包含异常 message（code=10043）
            assertNotNull(result.getErrorMessage());
            assertTrue(result.getErrorMessage().startsWith("讯飞 TTS 调用失败"),
                    "errorMessage 应以讯飞 TTS 调用失败开头，实际: " + result.getErrorMessage());
            assertTrue(result.getErrorMessage().contains("10043"),
                    "errorMessage 应包含错误码 10043，实际: " + result.getErrorMessage());
            // 错误码非 0 → 抛 ExecutionException → mapException 返回 UNKNOWN（非 Timeout/Connect）
            assertEquals(ProviderErrorType.UNKNOWN, result.getErrorType());
        }
    }

    @Test
    void synthesize_EmptyAudioBuffer_ReturnsEmptyAudioFailure() throws Exception {
        try (var mockedHttpClient = mockStatic(HttpClient.class)) {
            WebSocket.Builder mockBuilder = mock(WebSocket.Builder.class);
            WebSocket mockSocket = mock(WebSocket.class);
            mockHttpClientWithWebSocket(mockedHttpClient, mockBuilder);

            ArgumentCaptor<WebSocket.Listener> listenerCaptor = ArgumentCaptor.forClass(WebSocket.Listener.class);
            when(mockBuilder.buildAsync(any(), listenerCaptor.capture()))
                    .thenAnswer(invocation -> {
                        WebSocket.Listener listener = invocation.getArgument(1);
                        CompletableFuture.runAsync(() -> {
                            listener.onOpen(mockSocket);
                            // 立即收到结束帧，未收到任何音频数据
                            listener.onText(mockSocket,
                                    "{\"code\":0,\"data\":{\"audio\":\"\",\"status\":2}}", true);
                        });
                        return CompletableFuture.completedFuture(mockSocket);
                    });

            AudioSynthesisRequest request = new AudioSynthesisRequest();
            request.setText("测试空音频");

            AssetGenerationResult result = provider.synthesize(request);

            assertFalse(result.isSuccess());
            assertEquals(ProviderErrorType.INVALID_RESPONSE, result.getErrorType());
            assertEquals("EMPTY_AUDIO", result.getErrorCode());
        }
    }

    @Test
    void synthesize_OnErrorCallback_PropagatesAsFailure() throws Exception {
        try (var mockedHttpClient = mockStatic(HttpClient.class)) {
            WebSocket.Builder mockBuilder = mock(WebSocket.Builder.class);
            WebSocket mockSocket = mock(WebSocket.class);
            mockHttpClientWithWebSocket(mockedHttpClient, mockBuilder);

            ArgumentCaptor<WebSocket.Listener> listenerCaptor = ArgumentCaptor.forClass(WebSocket.Listener.class);
            when(mockBuilder.buildAsync(any(), listenerCaptor.capture()))
                    .thenAnswer(invocation -> {
                        WebSocket.Listener listener = invocation.getArgument(1);
                        CompletableFuture.runAsync(() -> {
                            listener.onOpen(mockSocket);
                            listener.onError(mockSocket, new java.io.IOException("connection reset"));
                        });
                        return CompletableFuture.completedFuture(mockSocket);
                    });

            AudioSynthesisRequest request = new AudioSynthesisRequest();
            request.setText("测试 onError");

            AssetGenerationResult result = provider.synthesize(request);

            assertFalse(result.isSuccess(), "onError 回调应导致失败");
            // IOException 也不是 Timeout/Connect，所以映射为 UNKNOWN
            assertEquals(ProviderErrorType.UNKNOWN, result.getErrorType());
            assertEquals("REMOTE_ERROR", result.getErrorCode());
        }
    }

    @Test
    void synthesize_DefaultVoiceUsedWhenRequestVoiceBlank() throws Exception {
        byte[] mp3Bytes = new byte[]{0x49, 0x44, 0x33, 0x10, 0x20};
        String audioBase64 = Base64.getEncoder().encodeToString(mp3Bytes);

        try (var mockedHttpClient = mockStatic(HttpClient.class)) {
            WebSocket.Builder mockBuilder = mock(WebSocket.Builder.class);
            WebSocket mockSocket = mock(WebSocket.class);
            mockHttpClientWithWebSocket(mockedHttpClient, mockBuilder);

            ArgumentCaptor<WebSocket.Listener> listenerCaptor = ArgumentCaptor.forClass(WebSocket.Listener.class);
            when(mockBuilder.buildAsync(any(), listenerCaptor.capture()))
                    .thenAnswer(invocation -> {
                        WebSocket.Listener listener = invocation.getArgument(1);
                        CompletableFuture.runAsync(() -> {
                            listener.onOpen(mockSocket);
                            listener.onText(mockSocket,
                                    "{\"code\":0,\"data\":{\"audio\":\"" + audioBase64 + "\",\"status\":2}}", true);
                        });
                        return CompletableFuture.completedFuture(mockSocket);
                    });

            AudioSynthesisRequest request = new AudioSynthesisRequest();
            request.setText("测试默认发音人");
            request.setVoice("");  // 空 → 应使用 properties.tts.voice

            AssetGenerationResult result = provider.synthesize(request);

            assertTrue(result.isSuccess());
            // metadata.voice 应为默认发音人 xiaoyan
            assertNotNull(result.getMetadata());
            assertEquals("xiaoyan", result.getMetadata().get("voice"));
            assertEquals("xiaoyan", result.getModel());
        }
    }

    @Test
    void synthesize_FragmentedTextFrames_AreAccumulatedBeforeParse() throws Exception {
        // 模拟讯飞返回大音频 base64 时被 WebSocket 协议拆分成 3 个 onText 分片：
        //   分片1: {"code":0,"data":{"audio":"AAAA  (last=false)
        //   分片2: BBBB                         (last=false)
        //   分片3: CCC","status":2}}             (last=true)
        // 必须累积后再解析 JSON，否则 Jackson 抛 JsonEOFException
        byte[] mp3Bytes = new byte[]{0x49, 0x44, 0x33, 0x10, 0x20, 0x30, 0x40, 0x50};
        String audioBase64 = Base64.getEncoder().encodeToString(mp3Bytes);
        String fullJson = "{\"code\":0,\"data\":{\"audio\":\"" + audioBase64 + "\",\"status\":2}}";
        int split1 = 20;
        int split2 = fullJson.length() - 10;
        String frag1 = fullJson.substring(0, split1);
        String frag2 = fullJson.substring(split1, split2);
        String frag3 = fullJson.substring(split2);

        try (var mockedHttpClient = mockStatic(HttpClient.class)) {
            WebSocket.Builder mockBuilder = mock(WebSocket.Builder.class);
            WebSocket mockSocket = mock(WebSocket.class);
            mockHttpClientWithWebSocket(mockedHttpClient, mockBuilder);

            ArgumentCaptor<WebSocket.Listener> listenerCaptor = ArgumentCaptor.forClass(WebSocket.Listener.class);
            when(mockBuilder.buildAsync(any(), listenerCaptor.capture()))
                    .thenAnswer(invocation -> {
                        WebSocket.Listener listener = invocation.getArgument(1);
                        CompletableFuture.runAsync(() -> {
                            listener.onOpen(mockSocket);
                            listener.onText(mockSocket, frag1, false);
                            listener.onText(mockSocket, frag2, false);
                            listener.onText(mockSocket, frag3, true);
                        });
                        return CompletableFuture.completedFuture(mockSocket);
                    });

            AudioSynthesisRequest request = new AudioSynthesisRequest();
            request.setText("测试分片");

            AssetGenerationResult result = provider.synthesize(request);

            assertTrue(result.isSuccess(), "分片累积后应能正确解析 JSON 并合成音频");
            assertArrayEquals(mp3Bytes, result.getContent(), "分片场景下音频字节应与原始一致");
        }
    }

    @Test
    void isAvailable_Configured_ReturnsTrue() {
        assertTrue(provider.isAvailable());
    }

    @Test
    void isAvailable_NotConfigured_ReturnsFalse() {
        properties.getTts().setEnabled(false);
        assertFalse(provider.isAvailable());
    }

    @Test
    void isAvailable_MissingApiKey_ReturnsFalse() {
        properties.setApiKey(null);
        assertFalse(provider.isAvailable());
    }
}
