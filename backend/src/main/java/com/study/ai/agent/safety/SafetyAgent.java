package com.study.ai.agent.safety;

import com.study.ai.agent.core.AgentNames;
import com.study.ai.agent.core.AgentStatus;
import com.study.ai.agent.core.AgentStep;
import com.study.ai.agent.resource.ResourceAgentContext;
import com.study.ai.agent.resource.ResourceAgentResult;
import com.study.ai.agent.tool.SafetyTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SafetyAgent {
    private final SafetyTools safetyTools;

    /**
     * Builds final quality checks and presentation notes for the resource package.
     *
     * @param context shared resource agent context
     * @param result resource package result to enrich
     * @return trace step for this agent
     */
    public AgentStep execute(ResourceAgentContext context, ResourceAgentResult result) {
        long start = System.currentTimeMillis();
        AgentStep step = new AgentStep();
        step.setStepId(UUID.randomUUID().toString());
        step.setAgent(AgentNames.SAFETY_AGENT);
        step.setDisplayName(AgentNames.SAFETY_AGENT_DISPLAY);
        step.setAction(AgentNames.ACTION_VALIDATE_PACKAGE);
        step.setStartTimeMs(start);

        try {
            result.setQualityChecks(safetyTools.buildQualityChecks(context.getMaterial(), context.getResources()));
            result.setPresentationNotes(safetyTools.buildPresentationNotes(context.getResources()));

            step.setStatus(AgentStatus.SUCCESS);
            step.setOutputSummary("资源包质量校验与说明生成完成");
        } catch (Exception e) {
            step.setStatus(AgentStatus.FAILED);
            step.setErrorCode("SAFETY_ERROR");
            step.setMessage(e.getMessage());
            log.error("SafetyAgent failed", e);
            // Non-critical, let it pass
        } finally {
            step.setEndTimeMs(System.currentTimeMillis());
            step.setDurationMs(step.getEndTimeMs() - start);
            step.setProgress(100);
        }
        return step;
    }
}
