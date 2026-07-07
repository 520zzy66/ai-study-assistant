package com.study.ai.agent.config;

import java.util.List;

/**
 * Agent 配置加载器
 *
 * <p>从 resources/agents/*.yml 加载所有 Agent 配置。
 * 应用启动时执行一次，配置缓存到内存。
 *
 * @author AI Study Assistant
 */
public class AgentConfigLoader {

    // TODO Phase 4: 实现
    // 使用 SnakeYAML 或 Spring @ConfigurationProperties 加载 YAML

    /**
     * 加载所有 Agent 配置
     */
    public List<AgentConfig> loadAll() {
        // TODO Phase 4: 扫描 resources/agents/ 目录 → 解析 YAML → 返回 AgentConfig 列表
        throw new UnsupportedOperationException("Phase 4 实现");
    }

    /**
     * 根据 ID 加载单个 Agent 配置
     */
    public AgentConfig loadById(String agentId) {
        // TODO Phase 4: 加载对应 YAML 文件
        throw new UnsupportedOperationException("Phase 4 实现");
    }
}
