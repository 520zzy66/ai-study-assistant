package com.study.ai.agent.resource;

import com.study.ai.agent.core.AgentNames;
import com.study.ai.agent.core.AgentProgressEvent;
import com.study.ai.agent.core.AgentStatus;
import com.study.ai.agent.core.AgentStep;
import com.study.ai.agent.core.AgentTrace;
import com.study.ai.agent.multimodal.MultimodalAgent;
import com.study.ai.agent.plan.PlanAgent;
import com.study.ai.agent.profile.ProfileAgent;
import com.study.ai.agent.quiz.QuizAgent;
import com.study.ai.agent.safety.SafetyAgent;
import com.study.ai.agent.summary.SummaryAgent;
import com.study.ai.agent.tool.SafetyTools;
import com.study.common.BusinessException;
import com.study.ai.validator.MaterialValidator;
import com.study.common.UserContext;
import com.study.dto.request.GenerateResourcePackageRequest;
import com.study.entity.LearningMaterial;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceOrchestratorAgent {

    private final ProfileAgent profileAgent;
    private final SummaryAgent summaryAgent;
    private final QuizAgent quizAgent;
    private final PlanAgent planAgent;
    private final MultimodalAgent multimodalAgent;
    private final SafetyAgent safetyAgent;
    private final MaterialValidator materialValidator;
    private final SafetyTools safetyTools;

    /**
     * Orchestrates the resource package pipeline with deterministic specialist agents.
     *
     * @param request resource package request
     * @param progressReporter progress callback for the async task facade
     * @return generated resource package result
     */
    public ResourceAgentResult orchestrate(GenerateResourcePackageRequest request, Consumer<AgentProgressEvent> progressReporter) {
        Long userId = UserContext.getCurrentUserId();
        safetyTools.validateSelectedResources(request);
        LearningMaterial material = materialValidator.validateAndGet(request.getMaterialId(), userId);

        ResourceAgentContext context = new ResourceAgentContext();
        context.setUserId(userId);
        context.setMaterialId(material.getId());
        context.setMaterial(material);
        context.setIncludeSummary(enabled(request.getIncludeSummary()));
        context.setIncludeMindMap(enabled(request.getIncludeMindMap()));
        context.setIncludeQuiz(enabled(request.getIncludeQuiz()));
        context.setIncludePlan(enabled(request.getIncludePlan()));
        context.setIncludeMultimodalScript(enabled(request.getIncludeMultimodalScript()));

        ResourceAgentResult result = new ResourceAgentResult();
        result.setPackageId(UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        result.setMaterialId(material.getId());
        result.setMaterialName(material.getOriginalName());
        result.setGeneratedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        List<ResourceManifestItem> manifest = new ArrayList<>();
        result.setResourceManifest(manifest);
        
        AgentTrace trace = new AgentTrace();
        trace.setTraceId(UUID.randomUUID().toString());
        trace.setStatus("running");
        trace.setStartTimeMs(System.currentTimeMillis());
        result.setAgentTrace(trace);

        report(progressReporter, 12, AgentNames.PROFILE_AGENT, AgentNames.ACTION_ANALYZE_PROFILE, "画像分析 Agent 正在读取学习画像...");
        
        // 1. Profile Agent
        try {
            AgentStep profileStep = profileAgent.execute(context, request);
            trace.addStep(profileStep);
        } catch (Exception e) {
            trace.setStatus("failed");
            trace.setEndTimeMs(System.currentTimeMillis());
            trace.setDurationMs(trace.getEndTimeMs() - trace.getStartTimeMs());
            throw e;
        }

        result.setGoal(context.getGoal());
        result.setDifficulty(context.getDifficulty());
        result.setExamDate(context.getExamDate());
        result.setDailyHours(context.getDailyHours());
        result.setProfile(context.getProfileText());

        // 2. Summary Agent (summary)
        report(progressReporter, 22, AgentNames.SUMMARY_AGENT, AgentNames.ACTION_GENERATE_SUMMARY, "课程理解 Agent 正在生成讲解文档...");
        ResourceManifestItem summaryManifest = new ResourceManifestItem();
        summaryManifest.setKey("summary");
        summaryManifest.setTitle("课程讲解文档");
        summaryManifest.setType("summary");
        summaryManifest.setRequested(enabled(context.getIncludeSummary()));
        manifest.add(summaryManifest);
        trace.addStep(summaryAgent.execute(context, summaryManifest));

        // 3. Summary Agent (mindMap)
        report(progressReporter, 38, AgentNames.SUMMARY_AGENT, AgentNames.ACTION_GENERATE_MIND_MAP, "知识组织 Agent 正在生成思维导图...");
        ResourceManifestItem mindMapManifest = new ResourceManifestItem();
        mindMapManifest.setKey("mindMap");
        mindMapManifest.setTitle("知识点思维导图");
        mindMapManifest.setType("mind_map");
        mindMapManifest.setRequested(enabled(context.getIncludeMindMap()));
        manifest.add(mindMapManifest);
        trace.addStep(summaryAgent.executeMindMap(context, mindMapManifest));

        // 4. Quiz Agent
        report(progressReporter, 54, AgentNames.QUIZ_AGENT, AgentNames.ACTION_GENERATE_QUIZ, "题库生成 Agent 正在构造个性化练习...");
        ResourceManifestItem quizManifest = new ResourceManifestItem();
        quizManifest.setKey("quiz");
        quizManifest.setTitle("个性化练习题库");
        quizManifest.setType("quiz");
        quizManifest.setRequested(enabled(context.getIncludeQuiz()));
        manifest.add(quizManifest);
        trace.addStep(quizAgent.execute(context, quizManifest));

        // 5. Plan Agent
        report(progressReporter, 70, AgentNames.PLAN_AGENT, AgentNames.ACTION_GENERATE_STUDY_PATH, "路径规划 Agent 正在生成学习路径...");
        ResourceManifestItem planManifest = new ResourceManifestItem();
        planManifest.setKey("plan");
        planManifest.setTitle("个性化学习路径");
        planManifest.setType("study_path");
        planManifest.setRequested(enabled(context.getIncludePlan()));
        manifest.add(planManifest);
        trace.addStep(planAgent.execute(context, planManifest));

        // 6. Multimodal Agent
        report(progressReporter, 84, AgentNames.MULTIMODAL_AGENT, AgentNames.ACTION_GENERATE_MULTIMODAL_SCRIPT, "多模态脚本 Agent 正在编排 PPT、图像、语音和微课脚本...");
        ResourceManifestItem multimodalManifest = new ResourceManifestItem();
        multimodalManifest.setKey("multimodalScript");
        multimodalManifest.setTitle("多模态资源脚本包");
        multimodalManifest.setType("multimodal_script");
        multimodalManifest.setRequested(enabled(context.getIncludeMultimodalScript()));
        manifest.add(multimodalManifest);
        trace.addStep(multimodalAgent.execute(context, multimodalManifest));

        // 7. Safety Agent
        report(progressReporter, 94, AgentNames.SAFETY_AGENT, AgentNames.ACTION_VALIDATE_PACKAGE, "资源设计 Agent 正在整理资源包清单和评审说明...");
        trace.addStep(safetyAgent.execute(context, result));

        result.setAgents(List.of(
                AgentNames.PROFILE_AGENT_DISPLAY,
                AgentNames.SUMMARY_AGENT_DISPLAY,
                "知识组织 Agent",
                AgentNames.QUIZ_AGENT_DISPLAY,
                AgentNames.PLAN_AGENT_DISPLAY,
                AgentNames.MULTIMODAL_AGENT_DISPLAY
        ));
        
        result.setAgentDetails(buildAgentDetails(context));
        result.setResources(context.getResources());

        int requestedCount = (int) manifest.stream().filter(item -> Boolean.TRUE.equals(item.getRequested())).count();
        long generatedCount = manifest.stream().filter(item -> Boolean.TRUE.equals(item.getGenerated())).count();
        boolean hasFailed = trace.getSteps().stream().anyMatch(s -> s.getStatus() == AgentStatus.FAILED);
        if (requestedCount > 0 && generatedCount == 0) {
            trace.setStatus("failed");
            trace.setEndTimeMs(System.currentTimeMillis());
            trace.setDurationMs(trace.getEndTimeMs() - trace.getStartTimeMs());
            throw new BusinessException(500, "资源包生成失败，请稍后重试");
        }
        trace.setStatus(hasFailed ? "partial_success" : "success");
        trace.setEndTimeMs(System.currentTimeMillis());
        trace.setDurationMs(trace.getEndTimeMs() - trace.getStartTimeMs());

        log.info("资源包生成完成: userId={}, materialId={}, traceStatus={}", userId, material.getId(), trace.getStatus());
        return result;
    }

    private void report(Consumer<AgentProgressEvent> reporter, int progress, String agent, String action, String message) {
        if (reporter != null) {
            reporter.accept(new AgentProgressEvent(progress, agent, action, message));
        }
    }

    private boolean enabled(Boolean value) {
        return value == null || Boolean.TRUE.equals(value);
    }

    private List<Map<String, Object>> buildAgentDetails(ResourceAgentContext context) {
        return List.of(
            Map.of(
                    "name", AgentNames.PROFILE_AGENT_DISPLAY,
                    "role", "读取用户画像、学习目标和偏好，确定资源生成方向",
                    "output", "profile"
            ),
            Map.of(
                    "name", AgentNames.SUMMARY_AGENT_DISPLAY,
                    "role", "围绕资料内容生成课程讲解和知识结构",
                    "output", context.getResources().containsKey("summary") ? "summary" : "skipped"
            ),
            Map.of(
                    "name", "知识组织 Agent",
                    "role", "把课程知识点组织为可视化导图",
                    "output", context.getResources().containsKey("mindMap") ? "mindMap" : "skipped"
            ),
            Map.of(
                    "name", AgentNames.QUIZ_AGENT_DISPLAY,
                    "role", "根据难度和资料内容生成练习题与解析",
                    "output", context.getResources().containsKey("quiz") ? "quiz" : "skipped"
            ),
            Map.of(
                    "name", AgentNames.PLAN_AGENT_DISPLAY,
                    "role", "按目标日期和每日时长生成动态学习路径",
                    "output", context.getResources().containsKey("plan") ? "plan" : "skipped"
            ),
            Map.of(
                    "name", AgentNames.MULTIMODAL_AGENT_DISPLAY,
                    "role", "输出 PPT、图像、语音、微课分镜和实操案例脚本",
                    "output", context.getResources().containsKey("multimodalScript") ? "multimodalScript" : "skipped"
            )
        );
    }
}
