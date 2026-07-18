package com.study.ai.agent.resource;

import com.study.ai.agent.core.AgentTrace;
import com.study.dto.vo.ResourceAssetVO;
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

    /** 多模态资产列表（音频 + 图片），spec §8.3。旧任务无此字段时前端按空数组处理。 */
    private List<ResourceAssetVO> assets;
}
