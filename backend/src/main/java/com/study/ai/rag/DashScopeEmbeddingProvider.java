package com.study.ai.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 阿里云百炼 Embedding Provider。
 *
 * <p>使用百炼 OpenAI 兼容 Embedding 接口调用 text-embedding-v4，
 * 默认输出 1024 维向量，与当前 pgvector 表结构一致。</p>
 */
@Slf4j
@Component
public class DashScopeEmbeddingProvider implements EmbeddingProvider {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_BATCH_SIZE = 10;

    @Value("${dashscope.embedding.api-key:${DASHSCOPE_API_KEY:}}")
    private String apiKey;

    @Value("${dashscope.embedding.endpoint:https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings}")
    private String endpoint;

    @Value("${dashscope.embedding.model:text-embedding-v4}")
    private String model;

    @Value("${dashscope.embedding.dimension:1024}")
    private int dimension;

    @Value("${dashscope.embedding.timeout-ms:30000}")
    private long timeoutMs;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public float[] embed(String text) {
        List<float[]> vectors = embedBatch(List.of(requireText(text)));
        return vectors.get(0);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("阿里云百炼 Embedding 未配置 DASHSCOPE_API_KEY");
        }

        List<float[]> vectors = new ArrayList<>(texts.size());
        for (int start = 0; start < texts.size(); start += MAX_BATCH_SIZE) {
            int end = Math.min(start + MAX_BATCH_SIZE, texts.size());
            List<String> batch = texts.subList(start, end).stream()
                    .map(this::requireText)
                    .toList();
            vectors.addAll(callApi(batch));
        }
        return vectors;
    }

    private List<float[]> callApi(List<String> texts) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("input", texts);
            body.put("dimensions", dimension);
            body.put("encoding_format", "float");

            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(timeoutMs))
                    .POST(HttpRequest.BodyPublishers.ofString(
                            MAPPER.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("DashScope embedding request failed: status={}", response.statusCode());
                throw new IllegalStateException("阿里云百炼 Embedding 请求失败，HTTP " + response.statusCode());
            }
            return parseVectors(response.body(), texts.size());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("阿里云百炼 Embedding 请求被中断", e);
        } catch (Exception e) {
            if (e instanceof IllegalStateException stateException) {
                throw stateException;
            }
            throw new IllegalStateException("阿里云百炼 Embedding 调用失败", e);
        }
    }

    private List<float[]> parseVectors(String responseBody, int expectedCount) throws Exception {
        JsonNode data = MAPPER.readTree(responseBody).path("data");
        if (!data.isArray() || data.size() != expectedCount) {
            throw new IllegalStateException("阿里云百炼 Embedding 返回数量异常");
        }

        List<float[]> vectors = new ArrayList<>(expectedCount);
        for (JsonNode item : data) {
            JsonNode embedding = item.path("embedding");
            if (!embedding.isArray() || embedding.size() != dimension) {
                throw new IllegalStateException("Embedding 维度异常，期望 " + dimension
                        + "，实际 " + embedding.size());
            }
            float[] vector = new float[dimension];
            for (int i = 0; i < dimension; i++) {
                vector[i] = (float) embedding.get(i).asDouble();
            }
            vectors.add(vector);
        }
        return vectors;
    }

    private String requireText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Embedding 文本不能为空");
        }
        return text;
    }
}
