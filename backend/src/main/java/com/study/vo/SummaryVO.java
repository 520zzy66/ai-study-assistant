package com.study.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档总结 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SummaryVO {

    /**
     * 资料ID
     */
    private Long materialId;

    /**
     * AI 生成的总结内容（Markdown 格式）
     */
    private String summary;
}
