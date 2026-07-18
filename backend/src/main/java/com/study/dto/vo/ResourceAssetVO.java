package com.study.dto.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资源资产视图对象（返回前端）。
 *
 * <p>不包含 {@code storagePath} 等磁盘敏感字段，文件访问通过
 * {@code previewUrl} / {@code downloadUrl} 走后端校验接口。
 */
@Data
public class ResourceAssetVO {

    /** 资产 UUID */
    private String assetId;

    /** 所属资源包 ID */
    private String packageId;

    /** 生成任务 ID */
    private String taskId;

    /** 关联资料 ID */
    private Long materialId;

    /** 资产类型：audio / image */
    private String assetType;

    /** 资产角色：podcast / cover / explanation */
    private String assetRole;

    /** 状态：pending / generating / success / failed / cancelled */
    private String status;

    /** 提供方：xfyun-tts / xfyun-tti / xfyun-hidream */
    private String provider;

    /** 模型标识 */
    private String model;

    /** 文件名 */
    private String fileName;

    /** Content-Type */
    private String contentType;

    /** 文件大小（字节） */
    private Long sizeBytes;

    /** 音频时长（秒） */
    private Integer durationSeconds;

    /** 图片宽度 */
    private Integer width;

    /** 图片高度 */
    private Integer height;

    /** 生成提示词摘要（脱敏后） */
    private String promptSummary;

    /** 失败错误码（业务可理解） */
    private String errorCode;

    /** 失败原因（业务可理解） */
    private String errorMessage;

    /** 预览 URL（inline 播放/显示） */
    private String previewUrl;

    /** 下载 URL（attachment） */
    private String downloadUrl;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
