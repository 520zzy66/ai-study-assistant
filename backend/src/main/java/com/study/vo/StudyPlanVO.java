package com.study.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学习计划 VO
 * 隐藏 userId、materialIds 等内部字段
 */
@Data
public class StudyPlanVO {

    private Long id;
    private String title;
    private String goal;
    private LocalDate examDate;
    private Integer dailyHours;
    private Integer totalDays;
    private String planContent;
    private LocalDateTime createTime;
}
