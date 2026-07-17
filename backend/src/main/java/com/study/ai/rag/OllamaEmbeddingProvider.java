package com.study.ai.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * Ollama EmbeddingProvider 实现（Spring AI 1.1.2）
 * 使用本地 Ollama 服务运行 bge-m3 模型生成向量
 *
 * <p>配置要求：
 * <ul>
 *   <li>Ollama 服务运行在 {@code spring.ai.ollama.base-url}（默认 http://localhost:11434）</li>
 *   <li>模型名称：{@code ai.embedding.model}（兼容原 {@code spring.ai.ollama.embedding.model}）</li>
 * </ul>
 *
 * <p><b>业务逻辑零影响</b>：此类纯技术实现，不涉及任何业务规则。
 */
@Slf4j
@Component
public class OllamaEmbeddingProvider implements EmbeddingProvider {

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ai.embedding.model:${spring.ai.ollama.embedding.model:bge-m3}}")
    private String embeddingModel;

    @Value("${ai.embedding.dimension:1024}")
    private int dimension;

    /** Ollama Embedding 模型（延迟初始化，避免启动时 Ollama 未就绪导致失败） */
    private OllamaEmbeddingModel ollamaEmbeddingModel;

    @PostConstruct
    public void init() {
        try {
            OllamaApi ollamaApi = OllamaApi.builder()
                    .baseUrl(ollamaBaseUrl)
                    .build();

            OllamaEmbeddingOptions options = OllamaEmbeddingOptions.builder()
                    .model(embeddingModel)
                    .build();

            this.ollamaEmbeddingModel = OllamaEmbeddingModel.builder()
                    .ollamaApi(ollamaApi)
                    .defaultOptions(options)
                    .build();

            log.info("Ollama EmbeddingProvider 初始化完成: model={}, baseUrl={}, dimension={}",
                    embeddingModel, ollamaBaseUrl, dimension);
        } catch (Exception e) {
            log.warn("Ollama EmbeddingProvider 初始化失败（将使用关键词匹配兜底）: {}", e.getMessage());
            this.ollamaEmbeddingModel = null;
        }
    }

    @Override
    public float[] embed(String text) {
        if (ollamaEmbeddingModel == null) {
            throw new IllegalStateException("Ollama Embedding 服务未就绪，请检查 Ollama 配置");
        }

        // B8 修复：失败时抛异常而非返回零向量，防止零向量污染 pgvector
        return ollamaEmbeddingModel.embed(text);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (ollamaEmbeddingModel == null) {
            throw new IllegalStateException("Ollama Embedding 服务未就绪，请检查 Ollama 配置");
        }

        // B8 修复：失败时抛异常而非返回零向量，防止零向量污染 pgvector
        return ollamaEmbeddingModel.embed(texts);
    }
}
