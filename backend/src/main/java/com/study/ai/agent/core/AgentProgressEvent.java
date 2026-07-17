package com.study.ai.agent.core;

/**
 * Agent 阶段进度事件
 * 
 * @param progress 当前进度 (0-100)
 * @param agent 当前执行的 Agent 机器名
 * @param action 当前执行的动作
 * @param message 进度信息描述
 */
public record AgentProgressEvent(
        int progress,
        String agent,
        String action,
        String message
) {}
