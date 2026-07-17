package com.study.ai.agent.summary;

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
public class SummaryAgent {

    private final ResourceGenerationTools tools;

    /**
     * Generates the personalized course explanation document when requested.
     *
     * @param context shared resource agent context
     * @param manifest resource manifest item to update
     * @return trace step for this agent action
     */
    public AgentStep execute(ResourceAgentContext context, ResourceManifestItem manifest) {
        long start = System.currentTimeMillis();
        AgentStep step = new AgentStep();
        step.setStepId(UUID.randomUUID().toString());
        step.setAgent(AgentNames.SUMMARY_AGENT);
        step.setDisplayName(AgentNames.SUMMARY_AGENT_DISPLAY);
        step.setAction(AgentNames.ACTION_GENERATE_SUMMARY);
        step.setStartTimeMs(start);

        if (context.getIncludeSummary() == null || !context.getIncludeSummary()) {
            step.setStatus(AgentStatus.SKIPPED);
            step.setOutputSummary("用户未选择生成该资源");
            step.setEndTimeMs(System.currentTimeMillis());
            step.setDurationMs(step.getEndTimeMs() - start);
            manifest.setStatus("skipped");
            manifest.setGenerated(false);
            return step;
        }

        try {
            String summary = tools.generateSummary(context.getMaterialId());
            context.setSummary(summary);
            Map<String, Object> resource = Map.of(
                    "type", "summary",
                    "title", "课程讲解文档",
                    "content", summary
            );
            context.getResources().put("summary", resource);

            step.setStatus(AgentStatus.SUCCESS);
            step.setOutputSummary("成功生成课程讲解文档");
            manifest.setStatus("success");
            manifest.setGenerated(true);
        } catch (Exception e) {
            step.setStatus(AgentStatus.FAILED);
            step.setErrorCode("SUMMARY_ERROR");
            step.setMessage(e.getMessage());
            manifest.setStatus("failed");
            manifest.setGenerated(false);
            manifest.setErrorCode("SUMMARY_ERROR");
            manifest.setMessage(e.getMessage());
            log.error("SummaryAgent failed", e);
        } finally {
            step.setEndTimeMs(System.currentTimeMillis());
            step.setDurationMs(step.getEndTimeMs() - start);
            step.setProgress(100);
        }
        return step;
    }

    /**
     * Generates the knowledge mind map when requested.
     *
     * @param context shared resource agent context
     * @param manifest resource manifest item to update
     * @return trace step for this agent action
     */
    public AgentStep executeMindMap(ResourceAgentContext context, ResourceManifestItem manifest) {
        long start = System.currentTimeMillis();
        AgentStep step = new AgentStep();
        step.setStepId(UUID.randomUUID().toString());
        step.setAgent(AgentNames.SUMMARY_AGENT);
        step.setDisplayName(AgentNames.SUMMARY_AGENT_DISPLAY);
        step.setAction(AgentNames.ACTION_GENERATE_MIND_MAP);
        step.setStartTimeMs(start);

        if (context.getIncludeMindMap() == null || !context.getIncludeMindMap()) {
            step.setStatus(AgentStatus.SKIPPED);
            step.setOutputSummary("用户未选择生成该资源");
            step.setEndTimeMs(System.currentTimeMillis());
            step.setDurationMs(step.getEndTimeMs() - start);
            manifest.setStatus("skipped");
            manifest.setGenerated(false);
            return step;
        }

        try {
            String mindMap = tools.generateMindMap(context.getMaterialId());
            Map<String, Object> resource = Map.of(
                    "type", "mind_map",
                    "title", "知识点思维导图",
                    "content", mindMap
            );
            context.getResources().put("mindMap", resource);

            step.setStatus(AgentStatus.SUCCESS);
            step.setOutputSummary("成功生成思维导图");
            manifest.setStatus("success");
            manifest.setGenerated(true);
        } catch (Exception e) {
            step.setStatus(AgentStatus.FAILED);
            step.setErrorCode("MINDMAP_ERROR");
            step.setMessage(e.getMessage());
            manifest.setStatus("failed");
            manifest.setGenerated(false);
            manifest.setErrorCode("MINDMAP_ERROR");
            manifest.setMessage(e.getMessage());
            log.error("SummaryAgent mind map failed", e);
        } finally {
            step.setEndTimeMs(System.currentTimeMillis());
            step.setDurationMs(step.getEndTimeMs() - start);
            step.setProgress(100);
        }
        return step;
    }
}
