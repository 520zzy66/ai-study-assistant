package com.study.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 异步任务 VO
 * 用于返回任务状态给前端，不暴露内部字段
 */
@Data
public class AiTaskVO {

    /** 任务UUID */
    private String taskId;

    /** 任务类型 summary/quiz/plan */
    private String type;

    /** 状态 pending/running/success/failed/cancelled */
    private String status;

    /** 进度 0-100 */
    private Integer progress;

    /** 状态描述 */
    private String message;

    /** 任务结果JSON */
    private String result;

    /** 错误信息 */
    private String errorMsg;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
