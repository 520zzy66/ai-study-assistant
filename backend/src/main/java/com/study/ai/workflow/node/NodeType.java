package com.study.ai.workflow.node;

/**
 * 工作流节点类型枚举
 *
 * <p>新架构仅保留实际使用的节点类型，图引擎相关类型已移除。
 *
 * @author AI Study Assistant
 */
public enum NodeType {

    /** 多模态预处理节点（文件解析 + 资料描述生成） */
    MULTIMODAL,

    /** 通用路由节点（三级路由决策 + 上下文注入） */
    GENERAL_ROUTER,

    /** 专家 Agent 节点（Spring AI @Tool 驱动的 ReAct 循环） */
    EXPERT_AGENT
}
