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
 * 临时资料切片，用于会话级 BM25 与向量检索。
 */
@Data
@TableName("temporary_material_chunk")
public class TemporaryMaterialChunk {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long temporaryMaterialId;
    private String uploadToken;
    private Long userId;
    private String conversationId;
    private Integer chunkIndex;
    private String content;
    private Integer chunkSize;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
