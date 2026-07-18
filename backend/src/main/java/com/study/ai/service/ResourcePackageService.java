package com.study.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.study.ai.agent.core.AgentProgressEvent;
import com.study.ai.agent.resource.ResourceAgentResult;
import com.study.ai.agent.resource.ResourceOrchestratorAgent;
import com.study.common.JsonUtils;
import com.study.dto.request.GenerateResourcePackageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Consumer;

/**
 * 个性化资源包编排服务。
 * 重构为 Agent 门面模式，将实现委托给 ResourceOrchestratorAgent。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourcePackageService {

    private final ResourceOrchestratorAgent orchestratorAgent;

    /**
     * Generates a resource package without progress callbacks.
     *
     * @param request resource package request
     * @return resource package result map
     */
    public Map<String, Object> generatePackage(GenerateResourcePackageRequest request) {
        return generatePackage(request, null, event -> {});
    }

    /**
     * Generates a resource package through the resource orchestrator agent.
     *
     * @param request resource package request
     * @param progressReporter progress event callback
     * @return resource package result map
     */
    public Map<String, Object> generatePackage(GenerateResourcePackageRequest request,
                                               Consumer<AgentProgressEvent> progressReporter) {
        return generatePackage(request, null, progressReporter);
    }

    /**
     * Generates a resource package through the resource orchestrator agent.
     *
     * @param request resource package request
     * @param taskId async task id (used by MultimodalAgent to persist resource_asset.task_id)
     * @param progressReporter progress event callback
     * @return resource package result map
     */
    public Map<String, Object> generatePackage(GenerateResourcePackageRequest request, String taskId,
                                               Consumer<AgentProgressEvent> progressReporter) {
        ResourceAgentResult agentResult = orchestratorAgent.orchestrate(request, taskId, progressReporter);
        // 将 POJO 序列化再反序列化为 Map，以兼容原有的 Map<String, Object> 返回签名
        String json = JsonUtils.toJson(agentResult);
        try {
            return JsonUtils.fromJson(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("ResourceAgentResult to Map conversion failed", e);
            throw new RuntimeException("序列化失败", e);
        }
    }
}
