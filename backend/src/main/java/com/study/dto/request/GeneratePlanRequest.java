package com.study.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * AI 学习计划生成请求
 */
@Data
public class GeneratePlanRequest {

    /** 学习目标 */
    @NotBlank(message = "学习目标不能为空")
    private String goal;

    /** 考试日期 yyyy-MM-dd */
    @NotBlank(message = "考试日期不能为空")
    private String examDate;

    /** 每日学习时长 */
    @Min(1) @Max(12)
    private Integer dailyHours;

    /** 关联资料ID列表 */
    private List<Long> materialIds;
}
