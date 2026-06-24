package com.study.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 题库实体
 */
@Data
@TableName("ai_question_bank")
public class AiQuestionBank {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 来源资料ID */
    private Long materialId;

    /** 批次ID */
    private String batchId;

    /** 批次名称 */
    private String batchName;

    /** 题目类型 choice/judge/short_answer */
    private String questionType;

    /** 难度 easy/medium/hard */
    private String difficulty;

    /** 题干 */
    private String question;

    /** 选项JSON（选择题专用） */
    private String options;

    /** 正确答案 */
    private String answer;

    /** 答案解析 */
    private String explanation;

    /** 收藏标记 0/1 */
    private Integer isFavorite;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
