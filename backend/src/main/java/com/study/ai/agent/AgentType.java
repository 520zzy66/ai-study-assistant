package com.study.ai.agent;

/**
 * Agent 类型枚举
 *
 * <p>当前架构中的 Agent 角色：
 * <ul>
 *   <li>ROUTER — 通用路由 Agent（GeneralNode 使用其 ChatClient）</li>
 *   <li>CIVIL_EXPERT / GRADUATE_EXPERT — 专家 Agent，负责专业解答</li>
 * </ul>
 *
 * @author AI Study Assistant
 */
public enum AgentType {

    /** 通用 Agent — 意图识别 + 路由分发 */
    ROUTER("通用助手", "🧭"),

    /** 考公专家 Agent */
    CIVIL_EXPERT("考公助手", "🏛️"),

    /** 考研专家 Agent */
    GRADUATE_EXPERT("考研助手", "🎓");

    private final String displayName;
    private final String icon;

    AgentType(String displayName, String icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getDisplayName() { return displayName; }
    public String getIcon() { return icon; }
}
