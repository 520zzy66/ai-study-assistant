package com.study.ai.agent.core;

import lombok.Data;

/**
 * 单个 Agent 或 Tool 执行步骤
 */
@Data
public class AgentStep {
    private String stepId;
    private String agent;
    private String displayName;
    private String action;
    private AgentStatus status;
    private Integer progress;
    private String message;
    private Long startTimeMs;
    private Long endTimeMs;
    private Long durationMs;
    private String outputSummary;
    private String errorCode;
}
