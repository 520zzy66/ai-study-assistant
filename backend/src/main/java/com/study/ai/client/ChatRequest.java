package com.study.ai.client;

import lombok.Builder;
import lombok.Data;

/**
 * AI 调用请求参数
 * 使用 Builder 模式统一管理各种调用参数组合
 */
@Data
@Builder
public class ChatRequest {

    /**
     * 用户提示词（必填）
     */
    private String prompt;

    /**
     * 系统提示词（可选）
     */
    private String systemPrompt;

    /**
     * 用户ID，用于对话记忆隔离（可选）
     * 设置后自动启用对话记忆
     */
    private Long userId;

    /**
     * 温度参数（可选，0.0~2.0）
     * null 使用默认值
     */
    private Double temperature;
}
