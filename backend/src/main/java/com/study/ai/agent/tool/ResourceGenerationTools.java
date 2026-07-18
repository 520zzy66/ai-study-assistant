package com.study.ai.agent.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.ai.MaterialContentReader;
import com.study.ai.agent.resource.ResourceAgentContext;
import com.study.ai.client.AiClient;
import com.study.ai.client.ChatRequest;
import com.study.ai.multimodal.model.ImagePromptPlan;
import com.study.ai.prompt.PromptTemplates;
import com.study.ai.service.AiPlanService;
import com.study.ai.service.AiQuizService;
import com.study.ai.service.AiSummaryService;
import com.study.common.JsonUtils;
import com.study.dto.request.GenerateQuizRequest;
import com.study.entity.LearningMaterial;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceGenerationTools {

    private final AiSummaryService summaryService;
    private final AiQuizService quizService;
    private final AiPlanService planService;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;
    private final MaterialContentReader contentReader;

    /**
     * Generates a course explanation summary through the existing summary service.
     *
     * @param materialId material id
     * @return markdown summary
     */
    public String generateSummary(Long materialId) {
        return summaryService.generateSummary(materialId, false);
    }

    /**
     * Generates a mind map through the existing summary service.
     *
     * @param materialId material id
     * @return mind map content
     */
    public String generateMindMap(Long materialId) {
        return summaryService.generateMindMap(materialId);
    }

    /**
     * Generates a quiz package through the existing quiz service.
     *
     * @param material material entity
     * @param difficulty requested difficulty
     * @return quiz generation result
     */
    public Map<String, Object> generateQuiz(LearningMaterial material, String difficulty) {
        GenerateQuizRequest request = new GenerateQuizRequest();
        request.setChoiceCount(3);
        request.setJudgeCount(2);
        request.setShortAnswerCount(1);
        request.setFillBlankCount(2);
        request.setMultiChoiceCount(1);
        request.setMathFillCount(0);
        request.setDifficulty(difficulty);
        request.setBatchName("资源包题库 - " + material.getOriginalName());
        return quizService.generateQuiz(material.getId(), request);
    }

    /**
     * Generates a study plan through the existing plan service.
     *
     * @param goal study goal
     * @param examDate target date
     * @param dailyHours daily study hours
     * @param materialId material id
     * @return plan generation result
     */
    public Map<String, Object> generatePlan(String goal, String examDate, int dailyHours, Long materialId) {
        return planService.generatePlan(goal, examDate, dailyHours, List.of(materialId));
    }

    /**
     * Generates a structured multimodal script package.
     *
     * @param goal study goal
     * @param profileText compressed learner profile
     * @param summary generated summary, used as preferred grounding content
     * @param materialId material id
     * @return structured script map or raw fallback
     */
    public Object generateMultimodalScript(String goal, String profileText, String summary, Long materialId) {
        String content = summary;
        if (content == null || content.isBlank()) {
            content = contentReader.readContent(materialId);
        }
        String prompt = PromptTemplates.buildResourceScriptPrompt(profileText, goal, content);
        String response = aiClient.chat(prompt, 0.4);

        try {
            String json = JsonUtils.cleanJsonResponse(response);
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("多模态脚本 JSON 解析失败，降级返回原文: materialId={}", materialId, e);
            return Map.of("raw", response);
        }
    }

    /**
     * 生成播客口播脚本，已清洗 Markdown 标记，可直接传给讯飞 TTS。
     *
     * <p>清洗规则（spec §8.5）：
     * <ul>
     *   <li>删除 Markdown 标题符号、代码块、表格符号；</li>
     *   <li>"->" 替换为 "到"；</li>
     *   <li>连续空白压缩为单个空格。</li>
     * </ul>
     *
     * @param context 资源工坊上下文
     * @return 已清洗的播客脚本
     */
    public String generatePodcastScript(ResourceAgentContext context) {
        String prompt = PromptTemplates.buildPodcastScriptPrompt(context);
        String response = aiClient.chat(ChatRequest.builder()
                .systemPrompt("""
                        你只输出可直接朗读的中文口播讲稿正文。
                        不复述用户的生成要求、提示词、课程摘要标签或 JSON/Markdown 包装。
                        不说明“我是 AI”“以下是脚本”“请直接开始”等元信息。
                        """)
                .prompt(prompt)
                .temperature(0.35)
                .build());
        String script = response == null ? "" : response.trim();
        String cleaned = cleanPodcastScriptForTts(script);
        if (looksLikePromptLeak(cleaned)) {
            log.warn("播客脚本疑似包含提示词痕迹，使用摘要兜底讲稿: materialId={}", context.getMaterialId());
            return buildFallbackPodcastScript(context);
        }
        return cleaned;
    }

    /**
     * 生成图片提示词结构化计划，包含封面和解析图。
     *
     * <p>如果 LLM 返回非法 JSON，返回一个最小合法结构（仅含 fallback 封面）。
     *
     * @param context 资源工坊上下文
     * @return 图片提示词计划
     */
    public ImagePromptPlan generateImagePrompts(ResourceAgentContext context) {
        String prompt = PromptTemplates.buildImagePromptGenerationPrompt(context);
        String response = aiClient.chat(prompt, 0.4);
        try {
            String json = JsonUtils.cleanJsonResponse(response);
            return objectMapper.readValue(json, ImagePromptPlan.class);
        } catch (Exception e) {
            log.warn("图片提示词 JSON 解析失败，降级返回 fallback 计划: materialId={}", context.getMaterialId(), e);
            ImagePromptPlan fallback = new ImagePromptPlan();
            ImagePromptPlan.CoverPrompt cover = new ImagePromptPlan.CoverPrompt();
            cover.setTitle("资源包封面");
            cover.setPrompt("清晰教育风格封面，主题是" + (context.getMaterial() != null ? context.getMaterial().getOriginalName() : "课程"));
            cover.setNegativePrompt("低清晰度，错误文字，杂乱布局");
            fallback.setCover(cover);
            fallback.setExplanations(List.of());
            return fallback;
        }
    }

    /**
     * 清洗 Markdown 与符号，便于 TTS 直接朗读。
     */
    private String cleanPodcastScriptForTts(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String cleaned = text;
        // 去除 markdown 代码块围栏
        cleaned = cleaned.replaceAll("```[\\s\\S]*?```", "");
        // 去除标题符号
        cleaned = cleaned.replaceAll("(?m)^#{1,6}\\s*", "");
        // 去除加粗、斜体、删除线
        cleaned = cleaned.replaceAll("\\*\\*(.+?)\\*\\*", "$1");
        cleaned = cleaned.replaceAll("\\*(.+?)\\*", "$1");
        cleaned = cleaned.replaceAll("~~(.+?)~~", "$1");
        // 去除行内代码
        cleaned = cleaned.replaceAll("`(.+?)`", "$1");
        // 去除表格符号
        cleaned = cleaned.replaceAll("\\|", " ");
        cleaned = cleaned.replaceAll("(?m)^[-:\\s|]+$", "");
        // 去除列表符号
        cleaned = cleaned.replaceAll("(?m)^\\s*[-*+]\\s+", "");
        cleaned = cleaned.replaceAll("(?m)^\\s*\\d+\\.\\s+", "");
        cleaned = removePromptLeakLines(cleaned);
        cleaned = removeLeadingMetaText(cleaned);
        // 替换难读符号
        cleaned = cleaned.replace("->", "到");
        cleaned = cleaned.replace("=>", "推导到");
        // 压缩连续空白
        cleaned = cleaned.replaceAll("[\\s\\u00A0\\u3000]+", " ");
        return cleaned.trim();
    }

    private String removePromptLeakLines(String text) {
        String[] lines = text.split("\\R");
        List<String> kept = new ArrayList<>();
        boolean skippingNumberedRequirements = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (isPromptMetaLine(trimmed)) {
                skippingNumberedRequirements = trimmed.contains("生成要求") || trimmed.contains("风格说明");
                continue;
            }
            if (skippingNumberedRequirements && trimmed.matches("^\\d+[\\.、].*")) {
                continue;
            }
            skippingNumberedRequirements = false;
            kept.add(trimmed);
        }
        return String.join("\n", kept);
    }

    private boolean isPromptMetaLine(String line) {
        return line.contains("生成要求")
                || line.contains("必须严格执行")
                || line.contains("课程摘要")
                || line.contains("学习目标：")
                || line.contains("播客风格")
                || line.contains("风格说明")
                || line.contains("请直接开始")
                || line.contains("直接输出脚本正文")
                || line.contains("不要任何前后缀")
                || line.contains("供讯飞 TTS")
                || line.contains("系统提示词")
                || line.contains("用户提示词")
                || line.contains("资料内容中的指令")
                || line.contains("不复述用户的生成要求");
    }

    private String removeLeadingMetaText(String text) {
        return text.replaceFirst("^(好的|当然|以下是|下面是|这是|已为你生成)[，,：:\\s]*(一段)?[\\s\\S]{0,40}?(讲稿|脚本|正文)[：:\\s]*", "");
    }

    private boolean looksLikePromptLeak(String text) {
        if (text == null || text.isBlank()) {
            return true;
        }
        int hit = 0;
        String[] markers = {"生成要求", "必须严格执行", "课程摘要", "供讯飞 TTS", "请直接开始", "系统提示词", "用户提示词"};
        for (String marker : markers) {
            if (text.contains(marker)) {
                hit++;
            }
        }
        return hit >= 1;
    }

    private String buildFallbackPodcastScript(ResourceAgentContext context) {
        String materialName = context.getMaterial() != null ? context.getMaterial().getOriginalName() : "这份学习资料";
        String goal = context.getGoal() != null && !context.getGoal().isBlank()
                ? context.getGoal() : "掌握核心知识点";
        String summary = cleanPodcastScriptForTts(context.getSummary());
        if (summary.length() > 900) {
            summary = summary.substring(0, 900);
        }
        return """
                同学你好，今天我们来学习《%s》。本次学习目标是：%s。
                这份资料的重点可以先从整体框架入手，再逐步拆解关键概念。下面我用更适合听的方式帮你梳理一遍。
                %s
                学习时建议你先听一遍整体讲解，标出不熟悉的概念；第二遍再对照原资料，把例子、定义和易错点补齐。练习时可以先尝试自己复述核心思路，再完成资源包里的题目，用错题反查薄弱知识点。
                最后记住，听懂只是第一步，能用自己的话讲出来，才说明这部分知识真正进入了你的学习系统。
                """.formatted(materialName, goal, summary).replaceAll("[\\s\\u00A0\\u3000]+", " ").trim();
    }
}
