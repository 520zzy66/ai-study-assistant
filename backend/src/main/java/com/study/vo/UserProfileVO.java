package com.study.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户画像 VO（返回给前端，不含内部字段）
 */
@Data
@Builder
public class UserProfileVO {

    private Long userId;
    private Integer age;
    private String education;
    private String targetExam;
    private String studySubject;
    private Integer dailyStudyGoalMinutes;
    private String preferredDifficulty;
    private String learningStyle;
    private List<String> weakPoints;
    private List<String> strongPoints;
    private Integer overallLevel;
    private Integer totalStudyDays;
    private Integer totalQuestions;
    private BigDecimal accuracyRate;
    private LocalDateTime lastActiveTime;
    private LocalDateTime updateTime;
}
