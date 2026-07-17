package com.study.ai.agent.core;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 记录完整 Agent 执行链路
 */
@Data
public class AgentTrace {
    private String traceId;
    private String status; // running/success/partial_success/failed/cancelled
    private Long startTimeMs;
    private Long endTimeMs;
    private Long durationMs;
    
    // 使用线程安全的 List，Phase A 虽然是串行，但符合文档“提供线程内安全的步骤更新方式”要求
    private List<AgentStep> steps = Collections.synchronizedList(new ArrayList<>());
    
    /**
     * Adds an agent step to this trace.
     *
     * @param step step to append
     */
    public void addStep(AgentStep step) {
        this.steps.add(step);
    }
}
