package com.study.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.study.common.JsonbTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 资源资产实体（音频 / 图片元数据）。
 *
 * <p>文件本体由 {@code ResourceAssetStorageService} 保存到本地存储目录，
 * 本表只保存元数据。{@code storagePath} 仅保存相对路径，禁止暴露绝对路径到前端。
 */
@Data
@TableName(value = "resource_asset", autoResultMap = true)
public class ResourceAsset {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 资产 UUID（前端下载用） */
    private String assetId;

    /** 所属资源包 ID */
    private String packageId;

    /** 生成任务 ID（可空） */
    private String taskId;

    /** 所属用户 ID */
    private Long userId;

    /** 关联资料 ID（可空） */
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

    /** 文件名（UUID 命名，不含用户输入） */
    private String fileName;

    /** Content-Type */
    private String contentType;

    /** 相对存储路径 */
    private String storagePath;

    /** 文件大小（字节） */
    private Long sizeBytes;

    /** 音频时长（秒，仅音频资产） */
    private Integer durationSeconds;

    /** 图片宽度（仅图片资产） */
    private Integer width;

    /** 图片高度（仅图片资产） */
    private Integer height;

    /** 生成提示词摘要（脱敏后） */
    private String promptSummary;

    /** 来源资源 key 列表（逗号分隔，如 summary,mindMap） */
    private String sourceKeys;

    /** 失败错误码（业务可理解） */
    private String errorCode;

    /** 失败原因（业务可理解） */
    private String errorMessage;

    /** 扩展元数据（JSONB） */
    @TableField(value = "metadata", typeHandler = JsonbTypeHandler.class)
    private Map<String, Object> metadata;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
