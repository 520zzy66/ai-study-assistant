package com.study.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * RAG 文档问答请求
 */
@Data
public class QaRequest {

    /** 资料ID */
    @NotNull(message = "资料ID不能为空")
    private Long materialId;

    /** 用户问题 */
    @NotBlank(message = "问题不能为空")
    private String question;
}
