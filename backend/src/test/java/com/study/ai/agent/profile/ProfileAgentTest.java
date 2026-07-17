package com.study.ai.agent.profile;

import com.study.ai.agent.core.AgentStep;
import com.study.ai.agent.resource.ResourceAgentContext;
import com.study.ai.agent.tool.ProfileTools;
import com.study.dto.request.GenerateResourcePackageRequest;
import com.study.entity.LearningMaterial;
import com.study.vo.UserProfileVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileAgentTest {

    @Mock
    private ProfileTools profileTools;

    @InjectMocks
    private ProfileAgent profileAgent;

    private ResourceAgentContext context;
    private GenerateResourcePackageRequest request;

    @BeforeEach
    void setUp() {
        context = new ResourceAgentContext();
        context.setUserId(1L);
        context.setMaterial(new LearningMaterial());

        request = new GenerateResourcePackageRequest();
        request.setGoal("掌握 Java 基础");
        request.setDifficulty("medium");
        request.setExamDate(LocalDate.now().plusDays(10).toString());
        request.setDailyHours(2);
    }

    @Test
    void execute_Success() {
        UserProfileVO profile = mock(UserProfileVO.class);
        when(profileTools.getOrCreateProfile(any())).thenReturn(profile);
        when(profileTools.resolveGoal(any(), any(), any())).thenReturn("掌握 Java 基础");
        when(profileTools.resolveDifficulty(any(), any())).thenReturn("medium");
        when(profileTools.resolveExamDate(any())).thenReturn(LocalDate.now().plusDays(10).toString());
        when(profileTools.resolveDailyHours(any(), any())).thenReturn(2);
        when(profileTools.formatProfile(any())).thenReturn("用户画像...");

        AgentStep step = profileAgent.execute(context, request);

        // Verify Step
        assertNotNull(step);
        assertEquals("success", step.getStatus().name().toLowerCase());
        assertNotNull(step.getDurationMs());
        assertNotNull(step.getOutputSummary());

        // Verify Context updates
        assertEquals("掌握 Java 基础", context.getGoal());
        assertEquals("medium", context.getDifficulty());
        assertNotNull(context.getExamDate());
        assertEquals(2, context.getDailyHours());
        assertEquals("用户画像...", context.getProfileText());
        
        verify(profileTools, times(1)).getOrCreateProfile(any());
    }

    @Test
    void execute_Exception_ReturnsFailedStep() {
        when(profileTools.getOrCreateProfile(any())).thenThrow(new RuntimeException("API 调用失败"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            profileAgent.execute(context, request);
        });

        assertEquals("API 调用失败", exception.getMessage());
    }
}
