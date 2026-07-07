package com.study.ai.agent.config;

import com.study.ai.agent.AgentType;

/**
 * Agent 配置 POJO
 *
 * <p>对应 resources/agents/ 下的 YAML 配置文件。
 * 每个 Agent 实例从对应的 YAML 加载配置。
 *
 * <p>配置项包括：
 * <ul>
 *   <li>id / name / description / icon — 基础标识</li>
 *   <li>model — 模型绑定（base-url, api-key, model, temperature）</li>
 *   <li>system-prompt-file — System Prompt 模板路径</li>
 *   <li>quiz — 出题默认配置</li>
 *   <li>domain-keywords — 领域专属关键词（用于 Layer 0 快速路由）</li>
 * </ul>
 *
 * @author AI Study Assistant
 */
public class AgentConfig {

    private String id;
    private AgentType type;
    private String name;
    private String description;
    private String icon;
    private int order;

    /** 模型配置 */
    private ModelConfig model;

    /** System Prompt 模板路径 */
    private String systemPromptFile;

    /**
     * 领域专属关键词（Layer 0 快速路由）
     * <p>Map：KEY=domain标识，VALUE=该领域关键词列表
     * <p>示例：{"CIVIL": ["申论","行测"], "GRADUATE": ["考研","高数"]}
     */
    private java.util.Map<String, java.util.List<String>> domainKeywords = new java.util.LinkedHashMap<>();

    /** 支持的领域列表 */
    private java.util.List<String> supportedDomains;

    /** 路由阈值配置 */
    private RoutingThresholds routing;

    /**
     * 路由阈值（Layer 4 分流决策）
     */
    public static class RoutingThresholds {
        /** ≥ 此阈值直接路由专家 */
        private double expertThreshold = 0.80;
        /** 此阈值-0.80 之间路由但附带备选 */
        private double suggestionThreshold = 0.60;

        public double getExpertThreshold() { return expertThreshold; }
        public void setExpertThreshold(double expertThreshold) { this.expertThreshold = expertThreshold; }
        public double getSuggestionThreshold() { return suggestionThreshold; }
        public void setSuggestionThreshold(double suggestionThreshold) { this.suggestionThreshold = suggestionThreshold; }
    }

    /**
     * 模型配置
     */
    public static class ModelConfig {
        private String baseUrl;
        private String apiKey;
        private String model;
        private double temperature = 0.7;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
    }

    // ===== Getters & Setters =====

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public AgentType getType() { return type; }
    public void setType(AgentType type) { this.type = type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }
    public ModelConfig getModel() { return model; }
    public void setModel(ModelConfig model) { this.model = model; }
    public String getSystemPromptFile() { return systemPromptFile; }
    public void setSystemPromptFile(String systemPromptFile) { this.systemPromptFile = systemPromptFile; }
    public java.util.Map<String, java.util.List<String>> getDomainKeywords() { return domainKeywords; }
    public void setDomainKeywords(java.util.Map<String, java.util.List<String>> domainKeywords) {
        this.domainKeywords = domainKeywords != null ? domainKeywords : new java.util.LinkedHashMap<>();
    }
    public java.util.List<String> getSupportedDomains() { return supportedDomains; }
    public void setSupportedDomains(java.util.List<String> supportedDomains) { this.supportedDomains = supportedDomains; }
    public RoutingThresholds getRouting() { return routing; }
    public void setRouting(RoutingThresholds routing) { this.routing = routing; }
}

