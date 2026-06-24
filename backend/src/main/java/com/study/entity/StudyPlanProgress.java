package com.study.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学习计划进度实体
 */
@Data
@TableName("study_plan_progress")
public class StudyPlanProgress {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 study_plan.id */
    private Long planId;

    /** 用户ID */
    private Long userId;

    /** 第几天（从1开始） */
    private Integer dayIndex;

    /** 是否完成 0/1 */
    private Integer completed;

    /** 实际学习时长 */
    private BigDecimal actualHours;

    /** 学习笔记 */
    private String note;

    /** 完成时间 */
    private LocalDateTime completeTime;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
