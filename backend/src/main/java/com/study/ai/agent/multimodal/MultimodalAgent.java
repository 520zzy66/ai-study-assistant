package com.study.ai.agent.multimodal;

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
public class MultimodalAgent {
    private final ResourceGenerationTools tools;

    /**
     * Generates the multimodal script package when requested.
     *
     * @param context shared resource agent context
     * @param manifest resource manifest item to update
     * @return trace step for this agent
     */
    public AgentStep execute(ResourceAgentContext context, ResourceManifestItem manifest) {
        long start = System.currentTimeMillis();
        AgentStep step = new AgentStep();
        step.setStepId(UUID.randomUUID().toString());
        step.setAgent(AgentNames.MULTIMODAL_AGENT);
        step.setDisplayName(AgentNames.MULTIMODAL_AGENT_DISPLAY);
        step.setAction(AgentNames.ACTION_GENERATE_MULTIMODAL_SCRIPT);
        step.setStartTimeMs(start);

        if (context.getIncludeMultimodalScript() == null || !context.getIncludeMultimodalScript()) {
            step.setStatus(AgentStatus.SKIPPED);
            step.setOutputSummary("用户未选择生成该资源");
            step.setEndTimeMs(System.currentTimeMillis());
            step.setDurationMs(step.getEndTimeMs() - start);
            manifest.setStatus("skipped");
            manifest.setGenerated(false);
            return step;
        }

        try {
            Object script = tools.generateMultimodalScript(context.getGoal(), context.getProfileText(), context.getSummary(), context.getMaterialId());
            Map<String, Object> resource = Map.of(
                    "type", "multimodal_script",
                    "title", "多模态资源脚本包",
                    "content", script
            );
            context.getResources().put("multimodalScript", resource);

            step.setStatus(AgentStatus.SUCCESS);
            step.setOutputSummary("成功生成多模态脚本");
            manifest.setStatus("success");
            manifest.setGenerated(true);
        } catch (Exception e) {
            step.setStatus(AgentStatus.FAILED);
            step.setErrorCode("MULTIMODAL_ERROR");
            step.setMessage(e.getMessage());
            manifest.setStatus("failed");
            manifest.setGenerated(false);
            manifest.setErrorCode("MULTIMODAL_ERROR");
            manifest.setMessage(e.getMessage());
            log.error("MultimodalAgent failed", e);
        } finally {
            step.setEndTimeMs(System.currentTimeMillis());
            step.setDurationMs(step.getEndTimeMs() - start);
            step.setProgress(100);
        }
        return step;
    }
}
