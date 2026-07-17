package com.study.ai.agent.core;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Agent 步骤执行状态
 */
public enum AgentStatus {
    PENDING("pending"),
    RUNNING("running"),
    SUCCESS("success"),
    FAILED("failed"),
    SKIPPED("skipped");

    private final String value;

    AgentStatus(String value) {
        this.value = value;
    }

    /**
     * Returns the lowercase value serialized to clients.
     *
     * @return lowercase status value
     */
    @JsonValue
    public String getValue() {
        return value;
    }
}
