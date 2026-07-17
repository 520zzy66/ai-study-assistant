package com.study.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 临时资料视图对象。
 */
@Data
public class TemporaryMaterialVO {

    private String uploadToken;
    private String conversationId;
    private String originalName;
    private String fileType;
    private Long fileSize;
    private String summary;
    private String status;
    private String errorMsg;
    private Integer chunkCount;
    private LocalDateTime expiresAt;
    private Long convertedMaterialId;
    private LocalDateTime createTime;
}
