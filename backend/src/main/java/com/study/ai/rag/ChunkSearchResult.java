package com.study.ai.rag;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 切片搜索结果
 */
@Data
@AllArgsConstructor
public class ChunkSearchResult {

    /** 切片ID */
    private Long chunkId;

    /** 切片序号 */
    private Integer chunkIndex;

    /** 切片内容 */
    private String content;

    /** 相似度分数 */
    private Double score;
}
