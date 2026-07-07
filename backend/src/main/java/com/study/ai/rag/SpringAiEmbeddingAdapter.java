package com.study.ai.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Spring AI EmbeddingModel 适配器（Spring AI 1.1.2）
 * 将项目自定义的 {@link EmbeddingProvider} 包装为 Spring AI 标准接口
 *
 * <p>作用：
 * <ul>
 *   <li>Spring AI VectorStore 自动注入此 Bean</li>
 *   <li>VectorStore.add() 时自动将文本转为向量</li>
 *   <li>项目仍保留 {@link EmbeddingProvider} 接口，便于未来切换实现方式</li>
 * </ul>
 *
 * <p><b>业务逻辑零影响</b>：此类纯技术适配，不涉及任何业务规则。
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class SpringAiEmbeddingAdapter implements EmbeddingModel {

    private final EmbeddingProvider embeddingProvider;

    /**
     * Spring AI 1.1.2 EmbeddingModel 核心方法
     *
     * @param request EmbeddingRequest（包含 texts + options）
     * @return EmbeddingResponse（包含 Embedding 列表，每个 Embedding 含 float[] 向量）
     */
    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<float[]> vectors = embeddingProvider.embedBatch(request.getInstructions());

        List<Embedding> embeddings = IntStream.range(0, vectors.size())
                .mapToObj(i -> new Embedding(vectors.get(i), i))
                .collect(Collectors.toList());

        return new EmbeddingResponse(embeddings);
    }

    /**
     * 嵌入单个 Document
     * Spring AI 1.1.2 中此方法为抽象方法，必须实现
     *
     * @param document Spring AI Document 对象
     * @return float[] 向量
     */
    @Override
    public float[] embed(Document document) {
        return embeddingProvider.embed(document.getText());
    }
}
