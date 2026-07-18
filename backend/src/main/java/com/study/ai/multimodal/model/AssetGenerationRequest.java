package com.study.ai.multimodal.model;

import lombok.Data;

/**
 * 资产生成请求基类，承载所有 Provider 共享的归属与上下文字段。
 */
@Data
public abstract class AssetGenerationRequest {

    /** 所属用户 ID */
    private Long userId;

    /** 关联资料 ID（可空） */
    private Long materialId;

    /** 所属资源包 ID */
    private String packageId;

    /** 生成任务 ID（可空） */
    private String taskId;

    /** 提示词摘要（脱敏后写入资产元数据） */
    private String promptSummary;
}
