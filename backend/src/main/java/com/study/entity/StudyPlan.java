package com.study.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学习计划实体
 */
@Data
@TableName("study_plan")
public class StudyPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 计划标题 */
    private String title;

    /** 学习目标 */
    private String goal;

    /** 考试日期 */
    private LocalDate examDate;

    /** 每日学习时长 */
    private Integer dailyHours;

    /** 总天数 */
    private Integer totalDays;

    /** 计划内容JSON */
    private String planContent;

    /** 关联资料ID列表 */
    private String materialIds;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
