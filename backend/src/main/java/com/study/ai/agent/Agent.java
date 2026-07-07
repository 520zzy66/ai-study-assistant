package com.study.ai.agent;

/**
 * Agent 抽象接口（保留供 AgentType 枚举引用，新代码使用 ExpertAgentService）。
 *
 * @author AI Study Assistant
 */
public interface Agent {

    /**
     * 获取 Agent 类型
     */
    AgentType getType();

    /**
     * 获取 Agent 名称（用于 SSE 事件展示）
     */
    String getName();

    /**
     * Agent 是否支持处理该领域
     *
     * @param domain 领域标识（如 "CIVIL"、"GRADUATE"）
     */
    boolean supportsDomain(String domain);
}
