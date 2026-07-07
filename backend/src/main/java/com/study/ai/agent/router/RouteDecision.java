package com.study.ai.agent.router;

/**
 * 路由决策结果
 *
 * <p>通用 Agent（Router）经过多层路由决策后输出的结构化结果。
 * 包含目标专家、意图描述、置信度等信息，供工作流的 ConditionNode 判断。
 *
 * <p>输出示例：
 * <pre>{@code
 * {
 *   "expertId": "CIVIL_EXPERT",
 *   "isExpert": true,
 *   "intent": "学生要求解析行测真题，需给出详细解题步骤和知识点讲解",
 *   "domain": "CIVIL",
 *   "confidence": 0.82,
 *   "reasoning": "用户问题包含'行测'关键词，Layer0 直接匹配到 CIVIL 领域"
 * }
 * }</pre>
 *
 * @author AI Study Assistant
 */
public class RouteDecision {

    /** 目标专家 ID（如 "CIVIL_EXPERT"、"GRADUATE_EXPERT"），非专家时为 null */
    private String expertId;

    /** 是否需要路由到专家 Agent */
    private boolean isExpert;

    /** 意图描述（传给专家 Agent 的结构化任务描述） */
    private String intent;

    /** 领域标识（如 "CIVIL"、"GRADUATE"、"GENERAL"） */
    private String domain;

    /** 校准后置信度（0.0-1.0），经过 Layer 3 置信度校准 */
    private double confidence;

    /** 路由推理过程（用于日志和调试） */
    private String reasoning;

    public RouteDecision() {}

    // ===== Getters & Setters =====

    public String getExpertId() { return expertId; }
    public void setExpertId(String expertId) { this.expertId = expertId; }

    public boolean isExpert() { return isExpert; }
    public void setExpert(boolean expert) { isExpert = expert; }

    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }
}
