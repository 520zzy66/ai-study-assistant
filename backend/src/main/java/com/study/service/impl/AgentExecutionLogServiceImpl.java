package com.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.study.ai.workflow.graph.WorkflowChatResult;
import com.study.dto.request.WorkflowChatRequest;
import com.study.entity.AgentExecutionLog;
import com.study.mapper.AgentExecutionLogMapper;
import com.study.service.AgentExecutionLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Default agent execution log service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentExecutionLogServiceImpl implements AgentExecutionLogService {

    private static final int MAX_QUERY_CHARS = 2000;
    private static final int MAX_SUMMARY_CHARS = 1200;
    private static final int MAX_ANSWER_CHARS = 8000;

    private final AgentExecutionLogMapper executionLogMapper;

    @Override
    public Long recordWorkflowExecution(Long userId, WorkflowChatRequest request,
                                        WorkflowChatResult result, Map<String, Object> routedState) {
        if (userId == null || userId <= 0 || request == null || result == null) {
            return null;
        }

        AgentExecutionLog executionLog = new AgentExecutionLog();
        executionLog.setUserId(userId);
        executionLog.setConversationId(result.getConversationId());
        executionLog.setQuestion(truncate(request.getQuestion(), MAX_QUERY_CHARS));
        executionLog.setRouteLevel(result.getRouteLevel());
        executionLog.setRoutedExpert(result.getRoutedExpert());
        executionLog.setRouteDomain(result.getRouteDomain());
        executionLog.setRouteIntent(truncate(result.getRouteIntent(), MAX_SUMMARY_CHARS));
        executionLog.setRouteConfidence(result.getRouteConfidence());
        executionLog.setRouting(buildRouting(request, result, routedState));
        executionLog.setExecutionChain(buildExecutionChain(result));
        executionLog.setFinalAnswer(truncate(result.getAnswer(), MAX_ANSWER_CHARS));
        executionLog.setFallback(result.isFallback() ? 1 : 0);
        executionLog.setErrorMessage(truncate(result.getErrorMessage(), 500));
        executionLog.setTotalDurationMs(result.getDurationMs());
        executionLog.setLlmCallCount(result.getLlmCallCount());
        executionLog.setToolCallCount(result.getToolCallCount());
        executionLog.setTotalTokens(0);
        try {
            executionLogMapper.insert(executionLog);
            return executionLog.getId();
        } catch (Exception e) {
            log.warn("Record workflow execution log failed: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void updateFeedback(Long executionId, Long userId, String feedback) {
        if (executionId == null || userId == null || feedback == null || feedback.isBlank()) {
            return;
        }
        String normalized = feedback.trim().toLowerCase();
        if (!"like".equals(normalized) && !"dislike".equals(normalized)) {
            return;
        }
        executionLogMapper.update(null, new LambdaUpdateWrapper<AgentExecutionLog>()
                .eq(AgentExecutionLog::getId, executionId)
                .eq(AgentExecutionLog::getUserId, userId)
                .set(AgentExecutionLog::getUserFeedback, normalized));
    }

    private Map<String, Object> buildRouting(WorkflowChatRequest request, WorkflowChatResult result,
                                             Map<String, Object> routedState) {
        Map<String, Object> routing = new LinkedHashMap<>();
        routing.put("routeLevel", result.getRouteLevel());
        routing.put("routedExpert", result.getRoutedExpert());
        routing.put("routeDomain", result.getRouteDomain());
        routing.put("routeIntent", result.getRouteIntent());
        routing.put("routeConfidence", result.getRouteConfidence());
        routing.put("conversationId", result.getConversationId());
        routing.put("materialId", request.getMaterialId());
        routing.put("hasMaterialText", request.getMaterialText() != null && !request.getMaterialText().isBlank());
        routing.put("routingSummary", truncate(value(routedState, "routingSummary"), MAX_SUMMARY_CHARS));
        routing.put("localAnswer", truncate(value(routedState, "localAnswer"), MAX_SUMMARY_CHARS));
        return routing;
    }

    private List<Map<String, Object>> buildExecutionChain(WorkflowChatResult result) {
        List<Map<String, Object>> chain = new ArrayList<>();
        List<String> path = result.getExecutionPath();
        if (path != null) {
            for (int i = 0; i < path.size(); i++) {
                Map<String, Object> step = new LinkedHashMap<>();
                step.put("order", i + 1);
                step.put("actor", path.get(i));
                step.put("phase", resolvePhase(path.get(i)));
                chain.add(step);
            }
        }
        List<Map<String, Object>> events = result.getExecutionEvents();
        if (events != null && !events.isEmpty()) {
            for (Map<String, Object> event : events) {
                Map<String, Object> step = new LinkedHashMap<>();
                step.put("phase", "event");
                step.put("event", event);
                chain.add(step);
            }
        }
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("phase", "metrics");
        metrics.put("durationMs", result.getDurationMs());
        metrics.put("llmCallCount", result.getLlmCallCount());
        metrics.put("toolCallCount", result.getToolCallCount());
        metrics.put("fallback", result.isFallback());
        chain.add(metrics);
        return chain;
    }

    private String resolvePhase(String actor) {
        if (actor == null) {
            return "unknown";
        }
        if (actor.startsWith("GeneralNode")) {
            return "routing";
        }
        if ("LocalAnswer".equals(actor)) {
            return "local_answer";
        }
        return actor.contains("fallback") ? "fallback" : "expert_answer";
    }

    private String value(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
