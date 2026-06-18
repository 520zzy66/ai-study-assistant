package com.study.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 异步任务实体
 */
@Data
@TableName("ai_task")
public class AiTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务UUID */
    private String taskId;

    /** 用户ID */
    private Long userId;

    /** 任务类型 material_process/summary/quiz/plan */
    private String type;

    /** 关联资源ID */
    private Long refId;

    /** 状态 pending/running/success/failed */
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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
