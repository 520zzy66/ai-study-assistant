package com.study.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话临时资料元数据。
 */
@Data
@TableName("temporary_material")
public class TemporaryMaterial {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String uploadToken;
    private Long userId;
    private String conversationId;
    private String originalName;
    private String storedName;
    private String fileType;
    private Long fileSize;
    private String filePath;
    private String summary;
    private String status;
    private String errorMsg;
    private Integer chunkCount;
    private LocalDateTime expiresAt;
    private Long convertedMaterialId;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
