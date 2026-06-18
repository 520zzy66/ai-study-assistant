package com.study.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 作答记录实体
 */
@Data
@TableName("ai_quiz_record")
public class AiQuizRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 批次ID */
    private String batchId;

    /** 题目ID */
    private Long questionId;

    /** 用户答案 */
    private String userAnswer;

    /** 是否正确 0/1 */
    private Integer isCorrect;

    /** 简答题AI评分 0-1 */
    private BigDecimal score;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
