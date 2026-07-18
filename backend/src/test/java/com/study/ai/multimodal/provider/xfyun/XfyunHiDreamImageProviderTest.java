package com.study.ai.multimodal.provider.xfyun;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.config.XfyunProperties;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class XfyunHiDreamImageProviderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void extractImageBytes_WhenResultContainsTaskIdBeforeImageBase64_SkipsNonImageText() throws Exception {
        byte[] pngBytes = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x10, 0x20, 0x30, 0x40
        };
        String imageBase64 = Base64.getEncoder().encodeToString(pngBytes);
        String resultJson = """
                {
                  "task_id": "task-abc-def-001",
                  "data": {
                    "image_base64": "%s"
                  }
                }
                """.formatted(imageBase64);
        String encodedResult = Base64.getEncoder().encodeToString(resultJson.getBytes(StandardCharsets.UTF_8));
        JsonNode textNode = MAPPER.getNodeFactory().textNode(encodedResult);

        XfyunHiDreamImageProvider provider = new XfyunHiDreamImageProvider(new XfyunProperties());
        Method method = XfyunHiDreamImageProvider.class.getDeclaredMethod("extractImageBytes", JsonNode.class);
        method.setAccessible(true);

        byte[] actual = (byte[]) method.invoke(provider, textNode);

        assertArrayEquals(pngBytes, actual);
    }

    @Test
    void extractImageBytes_WhenImageBase64IsUrlSafe_DecodesSuccessfully() throws Exception {
        byte[] pngBytes = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                (byte) 0xFF, (byte) 0xEE, 0x30, 0x40
        };
        String imageBase64 = Base64.getUrlEncoder().encodeToString(pngBytes);
        String resultJson = """
                {
                  "data": {
                    "imageBase64": "%s"
                  }
                }
                """.formatted(imageBase64);
        String encodedResult = Base64.getEncoder().encodeToString(resultJson.getBytes(StandardCharsets.UTF_8));
        JsonNode textNode = MAPPER.getNodeFactory().textNode(encodedResult);

        XfyunHiDreamImageProvider provider = new XfyunHiDreamImageProvider(new XfyunProperties());
        Method method = XfyunHiDreamImageProvider.class.getDeclaredMethod("extractImageBytes", JsonNode.class);
        method.setAccessible(true);

        byte[] actual = (byte[]) method.invoke(provider, textNode);

        assertArrayEquals(pngBytes, actual);
    }
}
