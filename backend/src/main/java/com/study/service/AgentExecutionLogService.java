package com.study.service;

import com.study.ai.workflow.graph.WorkflowChatResult;
import com.study.dto.request.WorkflowChatRequest;

import java.util.Map;

/**
 * Service for persisting agent workflow execution logs.
 */
public interface AgentExecutionLogService {

    /**
     * Records a completed workflow execution.
     *
     * @param userId      current user ID
     * @param request     original workflow request
     * @param result      workflow result
     * @param routedState state produced by the router
     * @return saved execution log ID, or null when no log is saved
     */
    Long recordWorkflowExecution(Long userId, WorkflowChatRequest request,
                                 WorkflowChatResult result, Map<String, Object> routedState);

    /**
     * Updates user feedback for an execution log.
     *
     * @param executionId execution log ID
     * @param userId      current user ID
     * @param feedback    feedback value
     */
    void updateFeedback(Long executionId, Long userId, String feedback);
}
