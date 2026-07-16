package com.study.ai.service;

import com.study.ai.MaterialContentReader;
import com.study.ai.agent.config.AgentClientFactory;
import com.study.ai.prompt.PromptTemplates;
import com.study.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import com.study.common.UserContext;

/**
 * AI 工具服务
 * 基于本地 qwen 模型，提供知识图谱、思维导图、学习计划生成能力
 *
 * <p>使用独立的 qwen ChatClient（通过 AgentClientFactory 创建），不干扰全局 MiMo 模型。
 * 所有方法返回 JSON 字符串，由调用方解析。
 */
@Slf4j
@Service
public class AiToolService {

    private final ChatClient qwenClient;
    private final MaterialContentReader contentReader;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int MAX_CONTENT_LEN = 12000;

    public AiToolService(AgentClientFactory agentClientFactory, MaterialContentReader contentReader) {
        this.qwenClient = agentClientFactory.getOrCreate("qwen-tools");
        this.contentReader = contentReader;
    }

    /**
     * 根据学习资料生成知识图谱（概念层级树）
     *
     * @param materialId 学习资料ID
     * @return JSON 字符串，格式为概念层级树
     */
    public String generateKnowledgeGraph(Long materialId) {
        log.info("生成知识图谱: materialId={}", materialId);
        String content = contentReader.readContent(materialId, MAX_CONTENT_LEN);
        if (content.isBlank()) {
            throw new BusinessException(400, "资料内容为空，无法生成知识图谱");
        }

        String prompt = PromptTemplates.buildKnowledgeGraphPrompt(content);
        String response = callQwen(prompt);
        String json = extractJson(response);

        if (json == null) {
            log.error("知识图谱 JSON 提取失败，原始响应: {}", response);
            throw new BusinessException(3005, "AI 返回格式异常，请重试");
        }
        return json;
    }

    /**
     * 根据学习资料生成思维导图
     *
     * @param materialId 学习资料ID
     * @return JSON 字符串，格式为树形结构
     */
    public String generateMindMap(Long materialId) {
        log.info("生成思维导图: materialId={}", materialId);
        String content = contentReader.readContent(materialId, MAX_CONTENT_LEN);
        if (content.isBlank()) {
            throw new BusinessException(400, "资料内容为空，无法生成思维导图");
        }

        String prompt = PromptTemplates.buildMindMapPrompt(content);
        String response = callQwen(prompt);
        String json = extractJson(response);

        if (json == null) {
            log.error("思维导图 JSON 提取失败，原始响应: {}", response);
            throw new BusinessException(3005, "AI 返回格式异常，请重试");
        }
        return json;
    }

    /**
     * 根据文件夹生成思维导图
     *
     * @param folderId 文件夹ID
     * @return JSON 字符串，格式为树形结构
     */
    public String generateFolderMindMap(Long folderId) {
        log.info("生成文件夹思维导图: folderId={}", folderId);
        Long userId = UserContext.getCurrentUserId();
        String content = contentReader.readFolderContent(folderId, userId);
        if (content.isBlank()) {
            throw new BusinessException(400, "文件夹资料内容为空，无法生成思维导图");
        }
        
        if (content.length() > MAX_CONTENT_LEN) {
            content = content.substring(0, MAX_CONTENT_LEN);
        }

        String prompt = PromptTemplates.buildMindMapPrompt(content);
        String response = callQwen(prompt);
        String json = extractJson(response);

        if (json == null) {
            log.error("思维导图 JSON 提取失败，原始响应: {}", response);
            throw new BusinessException(3005, "AI 返回格式异常，请重试");
        }
        return json;
    }

    /**
     * 根据学习目标生成个性化学习计划
     *
     * @param goal        学习目标
     * @param examDate    考试日期（yyyy-MM-dd）
     * @param dailyHours  每日可用学习小时数
     * @param materialIds 关联资料ID（逗号分隔），可为 null
     * @return JSON 字符串，格式为学习计划
     */
    public String generateStudyPlan(String goal, String examDate, int dailyHours, String materialIds) {
        log.info("生成学习计划: goal={}, examDate={}, dailyHours={}", goal, examDate, dailyHours);

        // 校验日期
        LocalDate exam;
        try {
            exam = LocalDate.parse(examDate, DATE_FMT);
        } catch (Exception e) {
            throw new BusinessException(400, "考试日期格式错误，应为 yyyy-MM-dd");
        }
        LocalDate today = LocalDate.now();
        if (exam.isBefore(today) || exam.isEqual(today)) {
            throw new BusinessException(400, "考试日期必须晚于今天");
        }

        long totalDays = ChronoUnit.DAYS.between(today, exam);
        if (totalDays > 90) {
            log.warn("学习周期超过90天({}), 建议拆分学习计划", totalDays);
        }

        // 构建 Prompt
        String prompt = PromptTemplates.buildPlanPrompt(
                goal, examDate, today.format(DATE_FMT), dailyHours,
                materialIds != null && !materialIds.isBlank() ? materialIds : "无");

        String response = callQwen(prompt);
        String json = extractJson(response);

        if (json == null) {
            log.error("学习计划 JSON 提取失败，原始响应: {}", response);
            throw new BusinessException(3005, "AI 返回格式异常，请重试");
        }
        return json;
    }

    /**
     * 调用 qwen ChatClient
     */
    private String callQwen(String prompt) {
        try {
            String response = qwenClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            log.info("qwen 响应成功，长度: {}", response != null ? response.length() : 0);
            return response;
        } catch (Exception e) {
            log.error("qwen 调用失败: {}", e.getMessage(), e);
            throw new BusinessException(3003, "本地 AI 服务不可用，请检查 Ollama 是否运行");
        }
    }

    /**
     * 从 AI 响应中提取 JSON（容错处理）
     *
     * <p>qwen 7B 模型可能在 JSON 前后添加说明文字，或使用 markdown 代码块包裹。
     * 此方法按优先级尝试多种提取策略。
     *
     * @param response AI 原始响应
     * @return 提取到的 JSON 字符串，失败返回 null
     */
    static String extractJson(String response) {
        if (response == null || response.isBlank()) {
            return null;
        }

        // 策略 1: 清理 markdown 代码块后直接作为 JSON
        String cleaned = response.replaceAll("```(json)?\\s*", "").trim();
        if (cleaned.startsWith("{") || cleaned.startsWith("[")) {
            return cleaned;
        }

        // 策略 2: 查找第一个 { 或 [，用括号匹配提取完整 JSON
        int objStart = cleaned.indexOf('{');
        int arrStart = cleaned.indexOf('[');
        int start;
        char openChar, closeChar;

        if (objStart >= 0 && (arrStart < 0 || objStart < arrStart)) {
            start = objStart;
            openChar = '{';
            closeChar = '}';
        } else if (arrStart >= 0) {
            start = arrStart;
            openChar = '[';
            closeChar = ']';
        } else {
            return null;
        }

        // 括号匹配（跳过字符串内的括号）
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = start; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (c == '\\' && inString) {
                escape = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (inString) continue;
            if (c == openChar) depth++;
            if (c == closeChar) {
                depth--;
                if (depth == 0) {
                    return cleaned.substring(start, i + 1);
                }
            }
        }

        return null;
    }
}
