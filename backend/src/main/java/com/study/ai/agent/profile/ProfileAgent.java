package com.study.ai.agent.profile;

import com.study.ai.agent.core.AgentNames;
import com.study.ai.agent.core.AgentStatus;
import com.study.ai.agent.core.AgentStep;
import com.study.ai.agent.resource.ResourceAgentContext;
import com.study.ai.agent.tool.ProfileTools;
import com.study.dto.request.GenerateResourcePackageRequest;
import com.study.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileAgent {

    private final ProfileTools profileTools;

    /**
     * Reads profile information and resolves resource generation parameters.
     *
     * @param context shared resource agent context
     * @param request resource package request
     * @return trace step for this agent
     */
    public AgentStep execute(ResourceAgentContext context, GenerateResourcePackageRequest request) {
        long start = System.currentTimeMillis();
        AgentStep step = new AgentStep();
        step.setStepId(UUID.randomUUID().toString());
        step.setAgent(AgentNames.PROFILE_AGENT);
        step.setDisplayName(AgentNames.PROFILE_AGENT_DISPLAY);
        step.setAction(AgentNames.ACTION_ANALYZE_PROFILE);
        step.setStartTimeMs(start);

        try {
            UserProfileVO profile = profileTools.getOrCreateProfile(context.getUserId());
            context.setProfile(profile);
            context.setGoal(profileTools.resolveGoal(request, context.getMaterial(), profile));
            context.setDifficulty(profileTools.resolveDifficulty(request, profile));
            context.setExamDate(profileTools.resolveExamDate(request));
            context.setDailyHours(profileTools.resolveDailyHours(request, profile));
            context.setProfileText(profileTools.formatProfile(profile));

            step.setStatus(AgentStatus.SUCCESS);
            step.setOutputSummary("获取用户画像，推断目标与偏好成功");
        } catch (Exception e) {
            step.setStatus(AgentStatus.FAILED);
            step.setErrorCode("PROFILE_ERROR");
            step.setMessage(e.getMessage());
            log.error("ProfileAgent failed", e);
            throw e;
        } finally {
            step.setEndTimeMs(System.currentTimeMillis());
            step.setDurationMs(step.getEndTimeMs() - start);
            step.setProgress(100);
        }
        return step;
    }
}
