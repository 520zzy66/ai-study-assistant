package com.study.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

import java.util.List;

/**
 * Workflow orchestrator chat request.
 */
@Data
public class WorkflowChatRequest {

    /** User question. */
    @NotBlank(message = "问题不能为空")
    @Size(max = 4000, message = "问题长度不能超过4000个字符")
    private String question;

    /** Optional related material ID. */
    private Long materialId;

    /** Optional server-issued token for one temporary conversation material. */
    @Size(max = 64, message = "临时资料令牌长度不能超过64个字符")
    private String temporaryMaterialToken;

    /** Optional material full text prepared by upstream modules. */
    @Size(max = 200000, message = "资料全文长度不能超过200000个字符")
    private String materialText;

    /** Optional material metadata or summary. */
    @Size(max = 4000, message = "资料元信息长度不能超过4000个字符")
    private String materialMeta;

    /** Optional material summary produced by a multimodal or preprocess node. */
    @Size(max = 8000, message = "资料摘要长度不能超过8000个字符")
    private String materialSummary;

    /** Conversation ID used by existing chat APIs. */
    private String conversationId;

    /** Session ID alias used by workflow specs. */
    private String sessionId;

    /** Optional frontend-provided conversation turns. */
    private List<QaRequest.ChatMessage> history;

    /**
     * 正式资料与临时资料每次只能关联一种。
     */
    @AssertTrue(message = "每次问答只能关联一份正式资料或一份临时资料")
    public boolean isSingleMaterialSelection() {
        return materialId == null || temporaryMaterialToken == null || temporaryMaterialToken.isBlank();
    }
}
