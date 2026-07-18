package com.study.ai.rag;

import java.util.List;

/**
 * Embedding 向量化接口
 * 负责将文本转换为向量，用于 RAG 语义检索
 *
 * 当前默认实现为阿里云百炼 text-embedding-v4，输出 1024 维向量。
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
