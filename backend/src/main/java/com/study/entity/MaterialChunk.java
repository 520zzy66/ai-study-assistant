package com.study.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档切片实体
 */
@Data
@TableName("material_chunk")
public class MaterialChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 资料ID */
    private Long materialId;

    /** 用户ID */
    private Long userId;

    /** 切片序号 */
    private Integer chunkIndex;

    /** 切片内容 */
    private String content;

    /** 向量数据 */
    private byte[] embedding;

    /** 切片大小（字符数） */
    private Integer chunkSize;

    /** 逻辑删除 0/1 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
