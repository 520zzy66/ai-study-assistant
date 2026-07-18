package com.study.ai.multimodal.model;

import lombok.Data;

import java.util.List;

/**
 * 图片提示词计划（LLM 结构化输出）。
 *
 * <p>对应 spec §8.6 由 LLM 生成的 JSON 结构，包含一张封面提示词和若干解析图提示词。
 */
@Data
public class ImagePromptPlan {

    /** 封面提示词 */
    private CoverPrompt cover;

    /** 解析图提示词列表 */
    private List<ExplanationPrompt> explanations;

    /** 封面提示词项 */
    @Data
    public static class CoverPrompt {
        /** 封面标题 */
        private String title;
        /** 封面正向提示词 */
        private String prompt;
        /** 封面反向提示词 */
        private String negativePrompt;
    }

    /** 解析图提示词项 */
    @Data
    public static class ExplanationPrompt {
        /** 对应知识点标题 */
        private String knowledgePoint;
        /** 正向提示词 */
        private String prompt;
        /** 反向提示词 */
        private String negativePrompt;
    }
}
