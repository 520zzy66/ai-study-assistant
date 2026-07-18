package com.study.ai.workflow.graph;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.study.ai.workflow.node.ExpertAgentNode;
import com.study.ai.workflow.node.GeneralNode;
import com.study.ai.workflow.node.LocalAnswerNode;
import com.study.ai.workflow.node.MultimodalNode;
import com.study.ai.workflow.service.ExpertAgentService;
import com.study.common.JsonUtils;
import com.study.dto.request.WorkflowChatRequest;
import com.study.entity.AiChatHistory;
import com.study.mapper.AiChatHistoryMapper;
import com.study.service.AgentExecutionLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;

/**
 * StateGraph 工作流服务 — 替代旧的 AiWorkflowOrchestrator。
 *
 * <p>构建并执行如下图结构：
 * <pre>
 * START → MultimodalNode → GeneralNode → (localAnswer | expertAgent) → END
 * </pre>
 *
 * <p>GeneralNode 负责路由决策 + 上下文注入 + 会话向量化 + 简单问题回答。
 * <p>LocalAnswerNode 返回 Level 0/1 的本地回答。
 * <p>ExpertAgentNode 内部路由到三个专家 Agent Service。
 */
@Slf4j
@Service
public class WorkflowGraphService {

    private static final String NODE_GENERAL = "general";
    private static final String NODE_MULTIMODAL = "multimodal";
    private static final String NODE_LOCAL_ANSWER = "localAnswer";
    private static final String NODE_EXPERT_AGENT = "expertAgent";

    private final GeneralNode generalNode;
    private final MultimodalNode multimodalNode;
    private final LocalAnswerNode localAnswerNode;
    private final ExpertAgentNode expertAgentNode;
    private final AiChatHistoryMapper chatHistoryMapper;
    private final AgentExecutionLogService executionLogService;

    public WorkflowGraphService(GeneralNode generalNode,
                                MultimodalNode multimodalNode,
                                LocalAnswerNode localAnswerNode,
                                ExpertAgentNode expertAgentNode,
                                AiChatHistoryMapper chatHistoryMapper,
                                AgentExecutionLogService executionLogService) {
        this.generalNode = generalNode;
        this.multimodalNode = multimodalNode;
        this.localAnswerNode = localAnswerNode;
        this.expertAgentNode = expertAgentNode;
        this.chatHistoryMapper = chatHistoryMapper;
        this.executionLogService = executionLogService;
    }

    /**
     * 执行工作流图（同步模式）。
     *
     * @param request 聊天请求
     * @param userId  当前用户 ID
     * @return 工作流结果
     */
    public WorkflowChatResult execute(WorkflowChatRequest request, Long userId) {
        long start = System.currentTimeMillis();
        String conversationId = resolveConversationId(request);

        try {
            // 1. 构建初始 state
            OverAllState initialState = buildInitialState(request, userId, conversationId);

            // 2. 构建并编译 StateGraph
            StateGraph graph = buildGraph();
            CompiledGraph compiled = graph.compile();

            // 3. 执行
            RunnableConfig config = RunnableConfig.builder().build();
            Optional<OverAllState> finalStateOpt = compiled.invoke(initialState, config);
            if (finalStateOpt.isEmpty()) {
                throw new IllegalStateException("StateGraph 执行返回空结果");
            }

            // 4. 提取结果
            OverAllState finalState = finalStateOpt.get();
            WorkflowChatResult result = extractResult(finalState);
            result.setDurationMs(System.currentTimeMillis() - start);
            result.setConversationId(conversationId);

            // 5. 保存历史 + 执行日志
            saveHistory(userId, request, result);
            persistExecutionLog(userId, request, result);

            log.info("[WorkflowGraph] 执行完成：level={}, expert={}, duration={}ms",
                    result.getRouteLevel(), result.getRoutedExpert(), result.getDurationMs());

            return result;

        } catch (Exception e) {
            log.error("[WorkflowGraph] 执行失败：{}", e.getMessage(), e);
            WorkflowChatResult result = buildFallbackResult(e.getMessage());
            result.setDurationMs(System.currentTimeMillis() - start);
            result.setConversationId(conversationId);
            return result;
        }
    }

    /**
     * 执行工作流图（SSE 流式模式）。
     *
     * <p>流程：
     * <ol>
     *   <li>GeneralNode 执行路由决策（同步）</li>
     *   <li>根据路由结果选择专家 Agent</li>
     *   <li>流式输出专家回答</li>
     * </ol>
     *
     * @param request 聊天请求
     * @param userId  当前用户 ID
     * @return SSE 文本流（逐 token 输出）
     */
    public Flux<String> executeStream(WorkflowChatRequest request, Long userId) {
        return Mono.fromCallable(() -> {
            // 1. 构建初始 state
            String conversationId = resolveConversationId(request);
            OverAllState initialState = buildInitialState(request, userId, conversationId);

            // 2. 构建仅包含 GeneralNode 的路由图（不执行 ExpertAgentNode）
            StateGraph routingGraph = buildRoutingGraph();
            CompiledGraph compiled = routingGraph.compile();
            RunnableConfig config = RunnableConfig.builder().build();

            Optional<OverAllState> routedStateOpt = compiled.invoke(initialState, config);
            if (routedStateOpt.isEmpty()) {
                throw new IllegalStateException("路由执行失败");
            }

            OverAllState routedState = routedStateOpt.get();
            String routedExpert = routedState.value(RouteKeys.ROUTED_EXPERT, "GENERAL_EXPERT");
            int routeLevel = routedState.value(RouteKeys.ROUTE_LEVEL, 2);
            // 读取本地回答（Level 0/1 由 LocalAnswerNode 写入 ANSWER）
            String localAnswer = routedState.value(RouteKeys.ANSWER, "");
            if (localAnswer == null || localAnswer.isBlank()) {
                localAnswer = routedState.value(RouteKeys.LOCAL_ANSWER, "");
            }

            return new Object[]{routedState, routedExpert, routeLevel, localAnswer, conversationId};
        })
        .flatMapMany(result -> {
            OverAllState routedState = (OverAllState) result[0];
            String routedExpert = (String) result[1];
            int routeLevel = (int) result[2];
            String localAnswer = (String) result[3];
            String conversationId = (String) result[4];

            final String finalConversationId = conversationId;
            final int finalRouteLevel = routeLevel;
            final String finalRoutedExpert = routedExpert;

            // 3. Level 0/1：本地回答，直接返回
            if (routeLevel < 2 && localAnswer != null && !localAnswer.isBlank()) {
                log.info("[WorkflowStream] 返回本地回答：level={}, length={}", routeLevel, localAnswer.length());

                return Flux.just(localAnswer)
                        .concatWithValues("[DONE]")
                        .doOnComplete(() -> {
                            WorkflowChatResult chatResult = new WorkflowChatResult();
                            chatResult.setRouteLevel(finalRouteLevel);
                            chatResult.setRoutedExpert(finalRoutedExpert);
                            chatResult.setConversationId(finalConversationId);
                            chatResult.setAnswer(localAnswer);
                            saveHistory(userId, request, chatResult);
                        });
            }

            // 4. Level 2：专家回答，使用 answerStream() 真正流式输出
            ExpertAgentService expertService = getExpertService(routedExpert);
            if (expertService == null) {
                log.warn("[WorkflowStream] 未找到专家服务：{}", routedExpert);
                String fallback = "抱歉，暂时无法处理该问题，请稍后重试。";
                return Flux.just("data: " + fallback + "\n\n", "data: [DONE]\n\n");
            }

            log.info("[WorkflowStream] 开始流式调用专家：{}", routedExpert);

            // 收集完整回答用于保存历史
            StringBuilder fullAnswer = new StringBuilder();

            return expertService.answerStream(routedState)
                    .filter(token -> token != null && !token.isBlank())
                    .doOnNext(token -> fullAnswer.append(token))
                    .concatWithValues("[DONE]")
                    .doOnComplete(() -> {
                        WorkflowChatResult chatResult = new WorkflowChatResult();
                        chatResult.setRouteLevel(finalRouteLevel);
                        chatResult.setRoutedExpert(finalRoutedExpert);
                        chatResult.setConversationId(finalConversationId);
                        chatResult.setAnswer(fullAnswer.toString());
                        saveHistory(userId, request, chatResult);
                        log.info("[WorkflowStream] 专家流式回答完成：expert={}, length={}",
                                finalRoutedExpert, fullAnswer.length());
                    });
        })
        .onErrorResume(e -> {
            log.error("[WorkflowStream] 执行失败：{}", e.getMessage(), e);
            return Flux.just(JsonUtils.toJson(Map.of("type", "error", "message", e.getMessage()), "{}"));
        })
        .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 获取专家服务实例。
     */
    private ExpertAgentService getExpertService(String expertId) {
        return expertAgentNode.getExpertService(expertId);
    }

    /**
     * 构建仅包含路由决策的 StateGraph（不执行 ExpertAgentNode）。
     * 用于流式模式：先路由，再按需调用专家的 answerStream()。
     */
    private StateGraph buildRoutingGraph() throws Exception {
        return new StateGraph()
                .addNode(NODE_MULTIMODAL, AsyncNodeAction.node_async(multimodalNode))
                .addNode(NODE_GENERAL, AsyncNodeAction.node_async(generalNode))
                .addNode(NODE_LOCAL_ANSWER, AsyncNodeAction.node_async(localAnswerNode))
                .addEdge(START, NODE_MULTIMODAL)
                .addEdge(NODE_MULTIMODAL, NODE_GENERAL)
                .addConditionalEdges(NODE_GENERAL,
                        AsyncEdgeAction.edge_async(this::routeDecision),
                        Map.of("local", NODE_LOCAL_ANSWER, "expert", END))
                .addEdge(NODE_LOCAL_ANSWER, END);
    }

    /**
     * 构建完整 StateGraph（同步模式使用）。
     */
    private StateGraph buildGraph() throws Exception {
        return new StateGraph()
                .addNode(NODE_MULTIMODAL, AsyncNodeAction.node_async(multimodalNode))
                .addNode(NODE_GENERAL, AsyncNodeAction.node_async(generalNode))
                .addNode(NODE_LOCAL_ANSWER, AsyncNodeAction.node_async(localAnswerNode))
                .addNode(NODE_EXPERT_AGENT, AsyncNodeAction.node_async(expertAgentNode))
                .addEdge(START, NODE_MULTIMODAL)
                .addEdge(NODE_MULTIMODAL, NODE_GENERAL)
                .addConditionalEdges(NODE_GENERAL,
                        AsyncEdgeAction.edge_async(this::routeDecision),
                        Map.of("local", NODE_LOCAL_ANSWER, "expert", NODE_EXPERT_AGENT))
                .addEdge(NODE_LOCAL_ANSWER, END)
                .addEdge(NODE_EXPERT_AGENT, END);
    }

    /**
     * 条件路由：根据 GeneralNode 输出选择下一个节点。
     */
    private String routeDecision(OverAllState state) {
        int routeLevel = state.<Integer>value(RouteKeys.ROUTE_LEVEL).orElse(2);
        String localAnswer = state.<String>value(RouteKeys.LOCAL_ANSWER).orElse("");
        if (routeLevel < 2 && localAnswer != null && !localAnswer.isBlank()) {
            return "local";
        }
        return "expert";
    }

    private OverAllState buildInitialState(WorkflowChatRequest request,
                                           Long userId,
                                           String conversationId) {
        Map<String, Object> state = new HashMap<>();
        state.put(RouteKeys.QUERY, request.getQuestion());
        state.put(RouteKeys.USER_ID, userId);
        state.put(RouteKeys.MATERIAL_ID, request.getMaterialId());
        state.put(RouteKeys.FOLDER_ID, request.getFolderId());
        state.put(RouteKeys.TEMPORARY_MATERIAL_TOKEN, request.getTemporaryMaterialToken());
        state.put(RouteKeys.MATERIAL_TEXT, request.getMaterialText());
        state.put(RouteKeys.MATERIAL_SUMMARY, request.getMaterialSummary());
        state.put(RouteKeys.MATERIAL_META, request.getMaterialMeta());
        state.put(RouteKeys.CONVERSATION_ID, conversationId);
        state.put(RouteKeys.FRONTEND_HISTORY, request.getHistory());
        return new OverAllState(state);
    }

    private WorkflowChatResult extractResult(OverAllState state) {
        WorkflowChatResult result = new WorkflowChatResult();
        result.setAnswer(state.<String>value(RouteKeys.ANSWER).orElse(""));
        result.setRouteLevel(state.<Integer>value(RouteKeys.ROUTE_LEVEL).orElse(2));
        result.setRoutedExpert(state.<String>value(RouteKeys.ROUTED_EXPERT).orElse(""));
        result.setRouteDomain(state.<String>value(RouteKeys.ROUTE_DOMAIN).orElse(""));
        result.setRouteIntent(state.<String>value(RouteKeys.ROUTE_INTENT).orElse(""));
        result.setRouteConfidence(state.<Double>value(RouteKeys.ROUTE_CONFIDENCE).orElse(0.0));
        result.setFallback(state.<Boolean>value(RouteKeys.FALLBACK).orElse(false));
        result.setErrorMessage(state.<String>value(RouteKeys.ERROR_MESSAGE).orElse(null));
        return result;
    }

    private WorkflowChatResult buildFallbackResult(String errorMessage) {
        WorkflowChatResult result = new WorkflowChatResult();
        result.setAnswer("工作流执行失败，请稍后重试。");
        result.setRouteLevel(2);
        result.setRoutedExpert("GENERAL_EXPERT");
        result.setRouteDomain("GENERAL");
        result.setFallback(true);
        result.setErrorMessage(errorMessage);
        return result;
    }

    private void saveHistory(Long userId, WorkflowChatRequest request, WorkflowChatResult result) {
        if (userId == null || userId <= 0) {
            return;
        }
        try {
            AiChatHistory history = new AiChatHistory();
            history.setUserId(userId);
            history.setChatType("workflow");
            history.setMaterialId(request.getMaterialId());
            history.setFolderId(request.getFolderId());
            history.setTemporaryMaterialToken(request.getTemporaryMaterialToken());
            history.setUserMessage(request.getQuestion());
            history.setAiResponse(result.getAnswer());
            history.setConversationId(result.getConversationId());
            chatHistoryMapper.insert(history);
        } catch (Exception e) {
            log.warn("[WorkflowGraph] 保存对话历史失败：{}", e.getMessage());
        }
    }

    private void persistExecutionLog(Long userId, WorkflowChatRequest request, WorkflowChatResult result) {
        try {
            Long executionId = executionLogService.recordWorkflowExecution(userId, request, result, Map.of());
            result.setExecutionId(executionId);
        } catch (Exception e) {
            log.warn("[WorkflowGraph] 保存执行日志失败：{}", e.getMessage());
        }
    }

    private String resolveConversationId(WorkflowChatRequest request) {
        if (request.getConversationId() != null && !request.getConversationId().isBlank()) {
            return request.getConversationId();
        }
        if (request.getSessionId() != null && !request.getSessionId().isBlank()) {
            return request.getSessionId();
        }
        return UUID.randomUUID().toString();
    }

    private Map<String, Object> doneEvent(WorkflowChatResult result) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "done");
        event.put("answer", result.getAnswer());
        event.put("level", result.getRouteLevel());
        event.put("agent", result.getRoutedExpert());
        event.put("duration_ms", result.getDurationMs());
        event.put("fallback", result.isFallback());
        event.put("conversationId", result.getConversationId());
        return event;
    }

    private String toSse(String eventName, Map<String, Object> event) {
        return "event: " + eventName + "\n"
                + "data: " + JsonUtils.toJson(event, "{}") + "\n\n";
    }
}
