package com.study.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资料列表 VO
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MaterialVO {

    private Long id;

    /** 原始文件名 */
    private String originalName;

    /** 文件类型 */
    private String fileType;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 分类 */
    private String category;

    /** 所属文件夹ID */
    private Long folderId;

    /** 所属文件夹名称 */
    private String folderName;

    /** AI摘要（列表中可能为null） */
    private String summary;

    /** 状态 */
    private String status;

    /** 切片数量 */
    private Integer chunkCount;

    /** 资料来源 user/system */
    private String source;

    /** 错误信息（status=failed 时有值） */
    private String errorMsg;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
