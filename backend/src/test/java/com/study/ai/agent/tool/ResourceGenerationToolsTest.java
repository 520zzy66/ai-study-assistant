package com.study.ai.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.ai.MaterialContentReader;
import com.study.ai.agent.resource.ResourceAgentContext;
import com.study.ai.client.AiClient;
import com.study.ai.client.ChatRequest;
import com.study.ai.service.AiPlanService;
import com.study.ai.service.AiQuizService;
import com.study.ai.service.AiSummaryService;
import com.study.entity.LearningMaterial;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResourceGenerationToolsTest {

    @Test
    void generatePodcastScript_WhenModelEchoesPrompt_UsesCleanFallback() {
        AiClient aiClient = mock(AiClient.class);
        when(aiClient.chat(any(ChatRequest.class))).thenReturn("""
                生成要求（必须严格执行）：
                1. 中文口播风格，自然、简洁，适合语音合成。
                课程摘要：
                这里才是资料相关内容。
                请直接开始：
                """);
        ResourceGenerationTools tools = new ResourceGenerationTools(
                mock(AiSummaryService.class),
                mock(AiQuizService.class),
                mock(AiPlanService.class),
                aiClient,
                new ObjectMapper(),
                mock(MaterialContentReader.class)
        );

        ResourceAgentContext context = new ResourceAgentContext();
        context.setMaterialId(9L);
        context.setGoal("理解二叉树遍历");
        context.setSummary("二叉树遍历包括前序、中序和后序。学习时要区分根节点的访问顺序。");
        LearningMaterial material = new LearningMaterial();
        material.setOriginalName("数据结构二叉树.md");
        context.setMaterial(material);

        String script = tools.generatePodcastScript(context);

        assertFalse(script.contains("生成要求"));
        assertFalse(script.contains("请直接开始"));
        assertTrue(script.contains("数据结构二叉树.md"));
        assertTrue(script.contains("二叉树遍历"));
    }
}
