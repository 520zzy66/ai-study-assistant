package com.study.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

/**
 * 更新用户画像请求 DTO
 *
 * <p>用户在前端设置页手动修改画像信息。
 * 所有字段均为空时视为无更新（空字段不覆盖已有值）。
 */
@Data
public class UpdateUserProfileRequest {

    /** 年龄（6~80） */
    @Min(value = 6, message = "年龄不能小于6岁")
    @Max(value = 80, message = "年龄不能大于80岁")
    private Integer age;

    /**
     * 学籍
     * <p>可选值：high_school, junior_college, undergraduate, master, doctor, working, fresh_graduate
     */
    @Pattern(regexp = "^(high_school|junior_college|undergraduate|master|doctor|working|fresh_graduate)?$",
            message = "无效的学籍类型")
    private String education;

    /** 目标考试（最多100字符） */
    @Size(max = 100, message = "目标考试最多100字符")
    private String targetExam;

    /** 学习科目（最多100字符） */
    @Size(max = 100, message = "学习科目最多100字符")
    private String studySubject;

    /** 每日学习目标（分钟，10~720） */
    @Min(value = 10, message = "每日学习目标最少10分钟")
    @Max(value = 720, message = "每日学习目标最多720分钟（12小时）")
    private Integer dailyStudyGoalMinutes;

    /** 难度偏好：easy / normal / hard */
    @Pattern(regexp = "^(easy|normal|hard)?$", message = "无效的难度偏好")
    private String preferredDifficulty;

    /** 学习风格：visual / auditory / practice */
    @Pattern(regexp = "^(visual|auditory|practice)?$", message = "无效的学习风格")
    private String learningStyle;

    /** 薄弱知识点列表（最多50个标签） */
    @Size(max = 50, message = "薄弱知识点最多50个")
    private List<@Size(max = 50) String> weakPoints;

    /** 擅长知识点列表（最多50个标签） */
    @Size(max = 50, message = "擅长知识点最多50个")
    private List<@Size(max = 50) String> strongPoints;
}
