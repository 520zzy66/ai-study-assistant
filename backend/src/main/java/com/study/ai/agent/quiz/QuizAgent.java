package com.study.ai.agent.quiz;

import com.study.ai.agent.core.AgentNames;
import com.study.ai.agent.core.AgentStatus;
import com.study.ai.agent.core.AgentStep;
import com.study.ai.agent.resource.ResourceAgentContext;
import com.study.ai.agent.resource.ResourceManifestItem;
import com.study.ai.agent.tool.ResourceGenerationTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuizAgent {
    private final ResourceGenerationTools tools;

    /**
     * Generates the personalized question bank when requested.
     *
     * @param context shared resource agent context
     * @param manifest resource manifest item to update
     * @return trace step for this agent
     */
    public AgentStep execute(ResourceAgentContext context, ResourceManifestItem manifest) {
        long start = System.currentTimeMillis();
        AgentStep step = new AgentStep();
        step.setStepId(UUID.randomUUID().toString());
        step.setAgent(AgentNames.QUIZ_AGENT);
        step.setDisplayName(AgentNames.QUIZ_AGENT_DISPLAY);
        step.setAction(AgentNames.ACTION_GENERATE_QUIZ);
        step.setStartTimeMs(start);

        if (context.getIncludeQuiz() == null || !context.getIncludeQuiz()) {
            step.setStatus(AgentStatus.SKIPPED);
            step.setOutputSummary("用户未选择生成该资源");
            step.setEndTimeMs(System.currentTimeMillis());
            step.setDurationMs(step.getEndTimeMs() - start);
            manifest.setStatus("skipped");
            manifest.setGenerated(false);
            return step;
        }

        try {
            Map<String, Object> quiz = tools.generateQuiz(context.getMaterial(), context.getDifficulty());
            Map<String, Object> resource = Map.of(
                    "type", "quiz",
                    "title", "个性化练习题库",
                    "content", quiz
            );
            context.getResources().put("quiz", resource);

            step.setStatus(AgentStatus.SUCCESS);
            step.setOutputSummary("成功生成题库");
            manifest.setStatus("success");
            manifest.setGenerated(true);
        } catch (Exception e) {
            step.setStatus(AgentStatus.FAILED);
            step.setErrorCode("QUIZ_ERROR");
            step.setMessage(e.getMessage());
            manifest.setStatus("failed");
            manifest.setGenerated(false);
            manifest.setErrorCode("QUIZ_ERROR");
            manifest.setMessage(e.getMessage());
            log.error("QuizAgent failed", e);
        } finally {
            step.setEndTimeMs(System.currentTimeMillis());
            step.setDurationMs(step.getEndTimeMs() - start);
            step.setProgress(100);
        }
        return step;
    }
}
