package com.study.ai.agent.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.ai.MaterialContentReader;
import com.study.ai.client.AiClient;
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
}
