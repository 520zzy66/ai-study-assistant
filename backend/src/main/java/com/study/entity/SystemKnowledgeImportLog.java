package com.study.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统知识库导入日志
 *
 * <p>用于实现知识库"只入库一次"的幂等逻辑。
 * 通过文件内容 MD5 判断文件是否已入库或内容是否有更新。
 *
 * @author AI Study Assistant
 */
@Data
@TableName("system_knowledge_import_log")
public class SystemKnowledgeImportLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文件相对路径（如 civil/xingce/判断推理/逻辑判断-经典真题.pdf），唯一索引 */
    private String filePath;

    /** 文件内容 MD5（64 字符），用于判断内容是否变更 */
    private String fileMd5;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 入库切片数量 */
    private Integer chunkCount;

    /** 导入批次号（版本标识，方便回滚） */
    private String importBatch;

    /** 所属的知识库根目录（如 resources/knowledge-bank） */
    private String knowledgeRoot;

    /** 逻辑删除 0/1 */
    @TableLogic
    private Integer deleted;

    /** 导入时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime importedAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
