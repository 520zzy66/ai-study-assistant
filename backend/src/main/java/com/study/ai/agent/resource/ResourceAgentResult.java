package com.study.ai.agent.resource;

import com.study.ai.agent.core.AgentTrace;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 资源工坊生成结果
 */
@Data
public class ResourceAgentResult {
    private String packageId;
    private Long materialId;
    private String materialName;
    private String generatedAt;
    private String goal;
    private String difficulty;
    private String examDate;
    private Integer dailyHours;
    private String profile;
    private List<String> agents;
    private List<Map<String, Object>> agentDetails;
    private List<ResourceManifestItem> resourceManifest;
    private Map<String, Object> qualityChecks;
    private List<String> presentationNotes;
    private AgentTrace agentTrace;
    private Map<String, Object> resources;
}
