package com.study.ai.memory;

import com.study.entity.UserMemoryEntry;
import com.study.service.UserMemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 用户记忆提取器
 *
 * <p>从 AI 对话/答题记录中提取有价值的用户记忆条目，写入 user_memory_entry 表。
 * 提取规则由 AI 完成：发送最近对话上下文 → AI 输出结构化记忆 JSON → 解析写入。
 *
 * <p><b>用户不可见</b>：此类记忆仅供系统 Prompt 注入使用，不暴露给前端。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserMemoryExtractor {

    private final UserMemoryService userMemoryService;

    /** 提取记忆的 ChatClient（延迟初始化，避免启动时 LLM 未就绪导致失败） */
    private ChatClient memoryChatClient;

    @Value("${ai.memory.extractor.enabled:true}")
    private boolean extractorEnabled;

    /**
     * 从对话记录中提取记忆条目
     *
     * <p>提取后自动写入数据库。如果提取失败，不影响主流程。
     *
     * @param userId        用户 ID
     * @param conversationText 对话文本（用户问题 + AI 回答拼接）
     */
    public void extractFromConversation(Long userId, String conversationText) {
        if (!extractorEnabled || conversationText == null || conversationText.isBlank()) {
            return;
        }

        try {
            List<UserMemoryEntry> entries = callExtractionLLM(userId, conversationText);
            if (!entries.isEmpty()) {
                userMemoryService.addMemories(entries);
                log.debug("从对话中提取 {} 条记忆: userId={}", entries.size(), userId);
            }
        } catch (Exception e) {
            log.warn("记忆提取失败（不影响主流程）: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 调用 LLM 提取记忆条目
     *
     * <p>AI 返回格式示例：
     * <pre>
     * [{"category":"knowledge","content":"用户对泰勒公式掌握不牢","importance":0.8}]
     * </pre>
     */
    private List<UserMemoryEntry> callExtractionLLM(Long userId, String conversationText) {
        if (memoryChatClient == null) {
            return List.of();
        }

        String systemPrompt = """
                你是一个记忆提取专家。从以下对话中提取关于用户的长期记忆条目。

                提取规则：
                1. 只提取有价值的记忆（学习目标、薄弱知识点、学习偏好、学习习惯等）
                2. 不要提取无关紧要的对话细节
                3. 每条记忆用一句话概括
                4. 每条记忆标注 category 和 importance

                category 可选值：
                - preference: 学习偏好（如"喜欢晚上学习"）
                - goal: 学习目标（如"目标是考研数学一"）
                - knowledge: 知识点掌握情况（如"泰勒公式掌握不牢"）
                - mistake: 常见错误类型（如"积分计算容易粗心"）
                - habit: 学习习惯（如"每天学习2小时"）

                importance 范围 0~1：
                - 1.0: 核心目标，永不淘汰
                - 0.7~0.9: 重要知识点/偏好
                - 0.4~0.6: 一般习惯/偏好
                - < 0.4: 低频信息

                严格返回 JSON 数组格式（不要包含 markdown 代码块）：
                [{"category":"knowledge","content":"xxx","importance":0.8}]

                如果没有值得提取的记忆，返回空数组：[]
                """;

        try {
            String response = memoryChatClient.prompt()
                    .system(systemPrompt)
                    .user(conversationText)
                    .options(OpenAiChatOptions.builder()
                            .temperature(0.3)
                            .build())
                    .call()
                    .content();

            return parseMemoryEntries(userId, response);
        } catch (Exception e) {
            log.warn("记忆提取 LLM 调用失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 解析 AI 返回的 JSON 记忆列表
     */
    private List<UserMemoryEntry> parseMemoryEntries(Long userId, String json) {
        List<UserMemoryEntry> entries = new ArrayList<>();

        // 移除可能的 markdown 代码块
        json = json.trim();
        if (json.startsWith("```")) {
            json = json.replaceAll("```json?\\s*", "").replaceAll("```\\s*$", "");
        }

        // 简易 JSON 数组解析（避免引入额外 JSON 库依赖）
        // 格式：[{"category":"knowledge","content":"xxx","importance":0.8}]
        try {
            if (json.startsWith("[") && json.endsWith("]")) {
                // 拆分各个对象
                String content = json.substring(1, json.length() - 1).trim();
                if (content.isEmpty()) {
                    return entries;
                }

                // 按 }{ 拆分
                String[] objects = content.split("(?<=\\}),?\\s*(?=\\{)");
                for (String obj : objects) {
                    obj = obj.trim().replaceAll("^,", "").replaceAll(",$", "").trim();
                    if (obj.isEmpty() || !obj.startsWith("{")) continue;

                    String category = extractJsonField(obj, "category");
                    String memoryContent = extractJsonField(obj, "content");
                    String importanceStr = extractJsonField(obj, "importance");

                    if (category != null && memoryContent != null) {
                        UserMemoryEntry entry = new UserMemoryEntry();
                        entry.setUserId(userId);
                        entry.setCategory(category);
                        entry.setContent(memoryContent);
                        entry.setImportance(importanceStr != null ? Float.parseFloat(importanceStr) : 0.5f);
                        entry.setSourceType("dialog");
                        entries.add(entry);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析记忆条目 JSON 失败: {}", e.getMessage());
        }

        return entries;
    }

    /**
     * 从 JSON 对象字符串中提取字段值
     */
    private String extractJsonField(String json, String field) {
        Pattern pattern = Pattern.compile("\"" + field + "\"\\s*:\\s*\"?([^,\"}]+)\"?");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * 初始化记忆提取的 ChatClient
     * 延迟注入，避免启动时 LLM 未就绪导致失败
     *
     * @param builder ChatClient.Builder
     */
    public void initChatClient(ChatClient.Builder builder) {
        try {
            this.memoryChatClient = builder.build();
        } catch (Exception e) {
            log.warn("记忆提取 ChatClient 初始化失败（记忆提取功能将不可用）: {}", e.getMessage());
        }
    }
}
