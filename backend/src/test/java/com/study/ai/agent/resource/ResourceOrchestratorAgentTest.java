package com.study.ai.agent.resource;

import com.study.ai.agent.core.AgentStatus;
import com.study.ai.agent.core.AgentStep;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceOrchestratorAgentTest {

    @Mock private ProfileAgent profileAgent;
    @Mock private SummaryAgent summaryAgent;
    @Mock private QuizAgent quizAgent;
    @Mock private PlanAgent planAgent;
    @Mock private MultimodalAgent multimodalAgent;
    @Mock private SafetyAgent safetyAgent;
    @Mock private MaterialValidator materialValidator;
    @Mock private SafetyTools safetyTools;

    @InjectMocks
    private ResourceOrchestratorAgent orchestratorAgent;

    @BeforeEach
    void setUp() {
        UserContext.setCurrentUser(new UserContext.UserInfo(1L, "test"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void orchestrate_Success() {
        GenerateResourcePackageRequest request = new GenerateResourcePackageRequest();
        request.setMaterialId(10L);
        request.setIncludeSummary(true);
        request.setIncludeMindMap(true);
        request.setIncludeQuiz(true);
        request.setIncludePlan(true);
        request.setIncludeMultimodalScript(true);

        LearningMaterial material = new LearningMaterial();
        material.setId(10L);
        material.setOriginalName("test.pdf");

        when(materialValidator.validateAndGet(10L, 1L)).thenReturn(material);

        when(profileAgent.execute(any(), any())).thenReturn(step(AgentStatus.SUCCESS));
        when(summaryAgent.execute(any(), any())).thenAnswer(invocation -> generatedStep(invocation.getArgument(1), AgentStatus.SUCCESS));
        when(summaryAgent.executeMindMap(any(), any())).thenAnswer(invocation -> generatedStep(invocation.getArgument(1), AgentStatus.SUCCESS));
        when(quizAgent.execute(any(), any())).thenAnswer(invocation -> generatedStep(invocation.getArgument(1), AgentStatus.SUCCESS));
        when(planAgent.execute(any(), any())).thenAnswer(invocation -> generatedStep(invocation.getArgument(1), AgentStatus.SUCCESS));
        when(multimodalAgent.execute(any(), any(), any())).thenAnswer(invocation -> generatedStep(invocation.getArgument(1), AgentStatus.SUCCESS));
        when(safetyAgent.execute(any(), any())).thenReturn(step(AgentStatus.SUCCESS));

        AtomicInteger eventCount = new AtomicInteger(0);
        ResourceAgentResult result = orchestratorAgent.orchestrate(request, event -> {
            assertNotNull(event.message());
            eventCount.incrementAndGet();
        });

        assertNotNull(result);
        assertEquals(10L, result.getMaterialId());
        assertEquals("success", result.getAgentTrace().getStatus());
        assertEquals(7, result.getAgentTrace().getSteps().size()); // 7 steps
        assertTrue(eventCount.get() > 0);

        verify(safetyTools, times(1)).validateSelectedResources(request);
        verify(profileAgent, times(1)).execute(any(), eq(request));
    }

    @Test
    void orchestrate_NullResourceFlags_DefaultsToEnabled() {
        GenerateResourcePackageRequest request = new GenerateResourcePackageRequest();
        request.setMaterialId(10L);

        LearningMaterial material = new LearningMaterial();
        material.setId(10L);
        material.setOriginalName("test.pdf");

        when(materialValidator.validateAndGet(10L, 1L)).thenReturn(material);
        when(profileAgent.execute(any(), any())).thenReturn(step(AgentStatus.SUCCESS));
        when(summaryAgent.execute(any(), any())).thenAnswer(invocation -> generatedStep(invocation.getArgument(1), AgentStatus.SUCCESS));
        when(summaryAgent.executeMindMap(any(), any())).thenAnswer(invocation -> generatedStep(invocation.getArgument(1), AgentStatus.SUCCESS));
        when(quizAgent.execute(any(), any())).thenAnswer(invocation -> generatedStep(invocation.getArgument(1), AgentStatus.SUCCESS));
        when(planAgent.execute(any(), any())).thenAnswer(invocation -> generatedStep(invocation.getArgument(1), AgentStatus.SUCCESS));
        when(multimodalAgent.execute(any(), any(), any())).thenAnswer(invocation -> generatedStep(invocation.getArgument(1), AgentStatus.SUCCESS));
        when(safetyAgent.execute(any(), any())).thenReturn(step(AgentStatus.SUCCESS));

        ResourceAgentResult result = orchestratorAgent.orchestrate(request, event -> {});

        assertEquals("success", result.getAgentTrace().getStatus());
        assertTrue(result.getResourceManifest().stream().allMatch(item -> Boolean.TRUE.equals(item.getRequested())));
        verify(summaryAgent, times(1)).execute(any(), any());
        verify(summaryAgent, times(1)).executeMindMap(any(), any());
        verify(quizAgent, times(1)).execute(any(), any());
        verify(planAgent, times(1)).execute(any(), any());
        verify(multimodalAgent, times(1)).execute(any(), any(), any());
    }

    @Test
    void orchestrate_AllRequestedResourcesFail_ThrowsBusinessException() {
        GenerateResourcePackageRequest request = new GenerateResourcePackageRequest();
        request.setMaterialId(10L);
        request.setIncludeSummary(true);
        request.setIncludeMindMap(false);
        request.setIncludeQuiz(false);
        request.setIncludePlan(false);
        request.setIncludeMultimodalScript(false);

        LearningMaterial material = new LearningMaterial();
        material.setId(10L);
        material.setOriginalName("test.pdf");

        when(materialValidator.validateAndGet(10L, 1L)).thenReturn(material);
        when(profileAgent.execute(any(), any())).thenReturn(step(AgentStatus.SUCCESS));
        when(summaryAgent.execute(any(), any())).thenAnswer(invocation -> generatedStep(invocation.getArgument(1), AgentStatus.FAILED));
        when(summaryAgent.executeMindMap(any(), any())).thenAnswer(invocation -> generatedStep(invocation.getArgument(1), AgentStatus.SKIPPED));
        when(quizAgent.execute(any(), any())).thenAnswer(invocation -> generatedStep(invocation.getArgument(1), AgentStatus.SKIPPED));
        when(planAgent.execute(any(), any())).thenAnswer(invocation -> generatedStep(invocation.getArgument(1), AgentStatus.SKIPPED));
        when(multimodalAgent.execute(any(), any(), any())).thenAnswer(invocation -> generatedStep(invocation.getArgument(1), AgentStatus.SKIPPED));
        when(safetyAgent.execute(any(), any())).thenReturn(step(AgentStatus.SUCCESS));

        assertThrows(BusinessException.class, () -> orchestratorAgent.orchestrate(request, event -> {}));
    }

    private AgentStep generatedStep(ResourceManifestItem manifest, AgentStatus status) {
        manifest.setGenerated(status == AgentStatus.SUCCESS);
        manifest.setStatus(status.getValue());
        return step(status);
    }

    private AgentStep step(AgentStatus status) {
        AgentStep step = new AgentStep();
        step.setStatus(status);
        return step;
    }
}
