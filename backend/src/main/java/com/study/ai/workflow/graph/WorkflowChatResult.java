package com.study.ai.workflow.graph;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Workflow chat response returned to the frontend.
 */
@Data
public class WorkflowChatResult {

    /** Persisted execution log ID. */
    private Long executionId;

    /** Final answer in Markdown. */
    private String answer;

    /** Route level: 0 local answer, 1 simple question, 2 expert analysis. */
    private int routeLevel;

    /** Routed expert ID, such as CIVIL_EXPERT, GRADUATE_EXPERT or GENERAL_EXPERT. */
    private String routedExpert;

    /** Route domain: CIVIL, GRADUATE or GENERAL. */
    private String routeDomain;

    /** Route intent description. */
    private String routeIntent;

    /** Route confidence. */
    private double routeConfidence;

    /** Conversation ID for follow-up turns. */
    private String conversationId;

    /** Execution duration in milliseconds. */
    private long durationMs;

    /** LLM call count tracked by the orchestrator. */
    private int llmCallCount;

    /** Real Spring AI tool call count observed from @Tool methods. */
    private int toolCallCount;

    /** Execution path summary. */
    private List<String> executionPath;

    /** Bounded sanitized execution events for audit logging. */
    private List<Map<String, Object>> executionEvents;

    /** Whether fallback text was returned because expert execution failed. */
    private boolean fallback;

    /** Error message when fallback is true. */
    private String errorMessage;
}
