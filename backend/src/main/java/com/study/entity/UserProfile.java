package com.study.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户画像实体（结构化长期记忆）
 *
 * <p>存储用户的学习偏好、能力评估、薄弱知识点等结构化数据。
 * 用户可在设置页手动修改，系统 AI 也会根据学习行为自动更新。
 */
@Data
@TableName("user_profile")
public class UserProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID（一对一关联） */
    private Long userId;

    /** 年龄 */
    private Integer age;

    /**
     * 学籍
     * <ul>
     *   <li>high_school — 高中</li>
     *   <li>junior_college — 大专</li>
     *   <li>undergraduate — 本科</li>
     *   <li>master — 硕士</li>
     *   <li>doctor — 博士</li>
     *   <li>working — 在职</li>
     *   <li>fresh_graduate — 应届生</li>
     * </ul>
     */
    private String education;

    /** 目标考试（如：考研数学一、考公、雅思） */
    private String targetExam;

    /** 学习科目（如：高等数学、英语、行测） */
    private String studySubject;

    /** 每日学习目标（分钟） */
    private Integer dailyStudyGoalMinutes;

    /** 难度偏好：easy / normal / hard */
    private String preferredDifficulty;

    /** 学习风格：visual / auditory / practice */
    private String learningStyle;

    /** 薄弱知识点列表（JSONB 列，JacksonTypeHandler 序列化） */
    @TableField(value = "weak_points", typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private List<String> weakPoints;

    /** 擅长知识点列表（JSONB 列，JacksonTypeHandler 序列化） */
    @TableField(value = "strong_points", typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private List<String> strongPoints;

    /** 综合水平评估 0~100 */
    private Integer overallLevel;

    /** 累计学习天数 */
    private Integer totalStudyDays;

    /** 累计答题数 */
    private Integer totalQuestions;

    /** 答题正确率 */
    private BigDecimal accuracyRate;

    /** 最近活跃时间 */
    private LocalDateTime lastActiveTime;

    /**
     * LLM 压缩的用户学习画像（500 字以内）
     * <p>基于最近 50 轮对话异步压缩生成，描述用户备考状态、薄弱点、偏好。
     * 为空表示尚未压缩或已过期需要重新压缩。
     */
    private String compressedProfile;

    /**
     * 压缩时的对话轮次号（用于判断是否过期）
     * <p>当 user_profile.total_questions - profileVersion >= 10 时触发重新压缩。
     */
    private Integer profileVersion;

    /** 画像压缩时间 */
    private LocalDateTime compressedAt;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
