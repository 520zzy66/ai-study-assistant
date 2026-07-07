package com.study.ai.rag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Embedding 维度契约测试。
 */
class EmbeddingProviderDimensionTest {

    private static final int EXPECTED_DIMENSION = 1024;

    @Test
    void embedShouldReturnConfiguredVectorDimension() {
        EmbeddingProvider provider = new FixedDimensionEmbeddingProvider(EXPECTED_DIMENSION);

        float[] vector = provider.embed("操作系统中的死锁条件");

        assertThat(vector).hasSize(EXPECTED_DIMENSION);
    }

    @Test
    void embedBatchShouldReturnOneVectorPerInputWithConfiguredDimension() {
        EmbeddingProvider provider = new FixedDimensionEmbeddingProvider(EXPECTED_DIMENSION);

        List<float[]> vectors = provider.embedBatch(List.of("进程调度", "内存分页", "文件系统"));

        assertThat(vectors).hasSize(3);
        assertThat(vectors).allSatisfy(vector -> assertThat(vector).hasSize(EXPECTED_DIMENSION));
    }

    @Test
    void springAiAdapterShouldPreserveProviderVectorDimensionForRequests() {
        SpringAiEmbeddingAdapter adapter = new SpringAiEmbeddingAdapter(
                new FixedDimensionEmbeddingProvider(EXPECTED_DIMENSION));

        EmbeddingResponse response = adapter.call(new EmbeddingRequest(List.of("RAG 混合检索"), null));

        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResult().getOutput()).hasSize(EXPECTED_DIMENSION);
    }

    @Test
    void springAiAdapterShouldPreserveProviderVectorDimensionForDocuments() {
        SpringAiEmbeddingAdapter adapter = new SpringAiEmbeddingAdapter(
                new FixedDimensionEmbeddingProvider(EXPECTED_DIMENSION));

        float[] vector = adapter.embed(new Document("AI 学习计划生成"));

        assertThat(vector).hasSize(EXPECTED_DIMENSION);
    }

    private static class FixedDimensionEmbeddingProvider implements EmbeddingProvider {

        private final int dimension;

        private FixedDimensionEmbeddingProvider(int dimension) {
            this.dimension = dimension;
        }

        @Override
        public float[] embed(String text) {
            float[] vector = new float[dimension];
            int seed = text == null ? 0 : text.hashCode();
            for (int i = 0; i < dimension; i++) {
                vector[i] = (seed + i) / 1_000_000F;
            }
            return vector;
        }

        @Override
        public List<float[]> embedBatch(List<String> texts) {
            return texts.stream()
                    .map(this::embed)
                    .toList();
        }
    }
}
