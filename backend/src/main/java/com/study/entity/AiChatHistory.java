package com.study.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 对话历史实体
 */
@Data
@TableName("ai_chat_history")
public class AiChatHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 对话类型 summary/qa/quiz/plan */
    private String chatType;

    /** 关联资料ID */
    private Long materialId;

    /** 会话临时资料令牌（最多保留7天） */
    private String temporaryMaterialToken;

    /** 用户输入 */
    private String userMessage;

    /** AI回复 */
    private String aiResponse;

    /** 批次ID（出题专用） */
    private String batchId;

    /** 会话ID（多轮对话专用） */
    private String conversationId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
