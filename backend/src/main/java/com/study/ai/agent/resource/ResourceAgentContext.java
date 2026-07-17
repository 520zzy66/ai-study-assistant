package com.study.ai.agent.resource;

import com.study.entity.LearningMaterial;
import com.study.vo.UserProfileVO;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 资源工坊任务专属 Agent 上下文
 */
@Data
public class ResourceAgentContext {
    private Long userId;
    private Long materialId;
    private String goal;
    private String examDate;
    private Integer dailyHours;
    private String difficulty;
    private Boolean includeSummary;
    private Boolean includeMindMap;
    private Boolean includeQuiz;
    private Boolean includePlan;
    private Boolean includeMultimodalScript;
    
    private UserProfileVO profile;
    private LearningMaterial material;
    private String profileText;
    private String summary;
    
    // 用于存放各 Agent 生成并互相共享的资源
    private Map<String, Object> resources = new LinkedHashMap<>();
}
