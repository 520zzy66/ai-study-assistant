package com.study.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学习资料实体
 */
@Data
@TableName("learning_material")
public class LearningMaterial {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 原始文件名 */
    private String originalName;

    /** 存储文件名（UUID） */
    private String storedName;

    /** 文件类型 pdf/doc/docx/txt */
    private String fileType;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文件存储路径 */
    private String filePath;

    /** 分类 */
    private String category;

    /** AI生成的摘要 */
    private String summary;

    /** 状态 processing/ready/failed */
    private String status;

    /** 错误信息 */
    private String errorMsg;

    /** 切片数量 */
    private Integer chunkCount;

    /** 逻辑删除 0/1 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
