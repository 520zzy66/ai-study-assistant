package com.study.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新学习计划进度请求
 */
@Data
public class UpdatePlanProgressRequest {

    /** 是否完成 */
    private Boolean completed;

    /** 实际学习时长（小时） */
    @Min(value = 0, message = "学习时长不能为负数")
    @Max(value = 24, message = "学习时长不能超过24小时")
    private BigDecimal actualHours;

    /** 学习笔记 */
    private String note;
}
