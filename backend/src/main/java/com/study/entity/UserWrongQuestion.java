package com.study.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 错题本实体
 */
@Data
@TableName("user_wrong_question")
public class UserWrongQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 关联题库ID */
    private Long questionId;

    /** 来源资料ID */
    private Long materialId;

    /** 题目类型 */
    private String questionType;

    /** 用户答案 */
    private String userAnswer;

    /** 正确答案 */
    private String correctAnswer;

    /** 错误次数 */
    private Integer wrongCount;

    /** 最近错误时间 */
    private LocalDateTime lastWrongTime;

    /** 是否已掌握 0/1 */
    private Integer isMastered;

    /** 掌握时间 */
    private LocalDateTime masterTime;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
