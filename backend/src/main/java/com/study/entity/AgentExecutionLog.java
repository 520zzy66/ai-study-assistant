package com.study.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.study.common.JsonbTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Agent execution audit log.
 */
@Data
@TableName(value = "agent_execution_log", autoResultMap = true)
public class AgentExecutionLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** User ID that triggered the workflow. */
    private Long userId;

    /** Conversation/session ID. */
    private String conversationId;

    /** Original user question. */
    private String question;

    /** Route level: 0 local, 1 simple, 2 expert. */
    private Integer routeLevel;

    /** Routed expert ID. */
    private String routedExpert;

    /** Route domain. */
    private String routeDomain;

    /** Route intent summary. */
    private String routeIntent;

    /** Route confidence score. */
    private Double routeConfidence;

    /** Routing details as JSON. */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private Map<String, Object> routing;

    /** Execution chain details as JSON. */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private List<Map<String, Object>> executionChain;

    /** Final answer snapshot for audit. */
    private String finalAnswer;

    /** Whether fallback behavior was used. */
    private Integer fallback;

    /** Error message when fallback is true. */
    private String errorMessage;

    /** Total duration in milliseconds. */
    private Long totalDurationMs;

    /** Tracked LLM call count. */
    private Integer llmCallCount;

    /** Tracked tool call count. */
    private Integer toolCallCount;

    /** Reserved token count. */
    private Integer totalTokens;

    /** User feedback: like/dislike or null. */
    private String userFeedback;

    /** Creation time. */
    private LocalDateTime createdAt;

    /** Update time. */
    private LocalDateTime updatedAt;
}
