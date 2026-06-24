package com.study.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * RAG 文档问答请求
 */
@Data
public class QaRequest {

    /** 资料ID（可选，不传则进行通用对话） */
    private Long materialId;

    /** 用户问题 */
    @NotBlank(message = "问题不能为空")
    private String question;

    /** 对话历史（用于多轮对话） */
    private List<ChatMessage> history;

    /**
     * 对话消息
     */
    @Data
    public static class ChatMessage {
        /** 角色：user 或 assistant */
        private String role;
        /** 消息内容 */
        private String content;
    }
}
