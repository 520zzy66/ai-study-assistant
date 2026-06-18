package com.study.ai.rag;

import java.util.List;

/**
 * Embedding 向量化接口
 * 负责将文本转换为向量，用于 RAG 语义检索
 *
 * 【Phase 4 MVP】暂不实现实际模型，使用关键词匹配作为替代方案。
 * 后续可集成：
 * - 本地 bge-small-zh（ONNX Runtime）
 * - 阿里通义 text-embedding-v2
 * - OpenAI text-embedding-3-small
 */
public interface EmbeddingProvider {

    /**
     * 将单条文本转为向量
     *
     * @param text 文本
     * @return 向量（float数组）
     */
    float[] embed(String text);

    /**
     * 批量将文本转为向量
     *
     * @param texts 文本列表
     * @return 向量列表
     */
    List<float[]> embedBatch(List<String> texts);
}
