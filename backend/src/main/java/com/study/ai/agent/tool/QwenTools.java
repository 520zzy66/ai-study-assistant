package com.study.ai.agent.tool;

import com.study.ai.service.AiToolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 基于本地 qwen 模型的 AI 工具集
 *
 * <p>提供知识图谱生成、思维导图生成、学习计划生成三个 Tool，
 * 供 Agent 在对话中按需调用。所有 Tool 返回 JSON 字符串。
 *
 * <p>底层调用 {@link AiToolService}，使用本地 Ollama qwen2.5:7b 模型。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QwenTools {

    private final AiToolService aiToolService;

    /**
     * 根据学习资料生成知识图谱（概念层级树）
     *
     * @param materialId 学习资料ID
     * @return JSON 格式的概念层级树
     */
    @Tool(description = "根据学习资料生成知识图谱（概念层级树），返回 JSON 树形结构。" +
            "输入为学习资料ID，输出为包含 name、relation、children 的层级树。")
    public String generateKnowledgeGraph(
            @ToolParam(description = "学习资料ID") Long materialId) {
        log.info("Tool 调用: generateKnowledgeGraph, materialId={}", materialId);
        try {
            return aiToolService.generateKnowledgeGraph(materialId);
        } catch (Exception e) {
            log.error("generateKnowledgeGraph 失败: {}", e.getMessage());
            return errorJson("知识图谱生成失败: " + e.getMessage());
        }
    }

    /**
     * 根据学习资料生成思维导图
     *
     * @param materialId 学习资料ID
     * @return JSON 格式的树形结构
     */
    @Tool(description = "根据学习资料生成思维导图，返回 JSON 树形结构。" +
            "输入为学习资料ID，输出为包含 name 和 children 的树形结构。")
    public String generateMindMap(
            @ToolParam(description = "学习资料ID") Long materialId) {
        log.info("Tool 调用: generateMindMap, materialId={}", materialId);
        try {
            return aiToolService.generateMindMap(materialId);
        } catch (Exception e) {
            log.error("generateMindMap 失败: {}", e.getMessage());
            return errorJson("思维导图生成失败: " + e.getMessage());
        }
    }

    /**
     * 根据学习目标和考试日期生成个性化学习计划
     *
     * @param goal        学习目标
     * @param examDate    考试日期（yyyy-MM-dd）
     * @param dailyHours  每日可用学习小时数
     * @param materialIds 关联资料ID（逗号分隔），可为空
     * @return JSON 格式的学习计划
     */
    @Tool(description = "根据学习目标和考试日期生成个性化学习计划。" +
            "输入学习目标、考试日期、每日学习时长和关联资料ID，输出 JSON 格式的学习计划。")
    public String generateStudyPlan(
            @ToolParam(description = "学习目标，如'考研数学'") String goal,
            @ToolParam(description = "考试日期，格式 yyyy-MM-dd") String examDate,
            @ToolParam(description = "每日可用学习小时数") int dailyHours,
            @ToolParam(description = "关联的学习资料ID，多个用逗号分隔，可为空") String materialIds) {
        log.info("Tool 调用: generateStudyPlan, goal={}, examDate={}, dailyHours={}", goal, examDate, dailyHours);
        try {
            return aiToolService.generateStudyPlan(goal, examDate, dailyHours, materialIds);
        } catch (Exception e) {
            log.error("generateStudyPlan 失败: {}", e.getMessage());
            return errorJson("学习计划生成失败: " + e.getMessage());
        }
    }

    /**
     * 构建错误 JSON 响应
     */
    private String errorJson(String message) {
        return "{\"error\": \"" + message.replace("\"", "\\\"") + "\"}";
    }
}
