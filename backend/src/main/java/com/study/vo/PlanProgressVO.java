package com.study.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学习计划进度 VO
 */
@Data
public class PlanProgressVO {

    /** 计划ID */
    private Long planId;

    /** 第几天 */
    private Integer dayIndex;

    /** 是否完成 */
    private Boolean completed;

    /** 实际学习时长 */
    private BigDecimal actualHours;

    /** 学习笔记 */
    private String note;

    /** 完成时间 */
    private LocalDateTime completeTime;

    /** 总天数 */
    private Integer totalDays;

    /** 已完成天数 */
    private Long completedDays;

    /** 完成百分比 */
    private Integer progressPercent;
}
