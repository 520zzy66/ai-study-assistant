package com.study.ai.agent.multimodal;

import com.study.ai.agent.core.AgentStatus;
import com.study.ai.agent.core.AgentStep;
import com.study.ai.agent.resource.ResourceAgentContext;
import com.study.ai.agent.resource.ResourceManifestItem;
import com.study.ai.agent.tool.ResourceGenerationTools;
import com.study.ai.multimodal.model.ImagePromptPlan;
import com.study.ai.multimodal.service.ResourceAssetService;
import com.study.config.XfyunProperties;
import com.study.dto.vo.ResourceAssetVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 多模态 Agent 资产生成测试（spec §13.1）。
 *
 * <p>覆盖点：开关关闭不调用 Provider、开关打开写入 assets、Provider 失败不拖垮资源包。
 */
@ExtendWith(MockitoExtension.class)
class MultimodalAgentAssetTest {

    @Mock private ResourceGenerationTools tools;
    @Mock private ResourceAssetService assetService;
    @Mock private XfyunProperties xfyunProperties;

    @InjectMocks
    private MultimodalAgent multimodalAgent;

    private ResourceAgentContext context;
    private ResourceManifestItem manifest;

    @BeforeEach
    void setUp() {
        context = new ResourceAgentContext();
        context.setUserId(1L);
        context.setMaterialId(100L);
        context.setPackageId("pkg-1");
        context.setTaskId("task-1");
        context.setGoal("掌握 Java 基础");
        context.setProfileText("用户画像");
        context.setSummary("学习总结");
        context.setIncludeMultimodalScript(true);

        manifest = new ResourceManifestItem();
        manifest.setKey("multimodalScript");
        manifest.setTitle("多模态资源脚本包");
        manifest.setType("multimodal_script");
        manifest.setRequested(true);

        // 默认 XfyunProperties 配置（lenient：部分测试不进入图片生成路径）
        XfyunProperties.Image imageConfig = new XfyunProperties.Image();
        imageConfig.setDefaultSize("512x512");
        imageConfig.setMaxImageCount(4);
        lenient().when(xfyunProperties.getImage()).thenReturn(imageConfig);
    }

    // ============== 开关关闭场景 ==============

    @Test
    void execute_IncludeMultimodalScriptFalse_ReturnsSkipped() {
        context.setIncludeMultimodalScript(false);

        AgentStep step = multimodalAgent.execute(context, manifest, null);

        assertEquals(AgentStatus.SKIPPED, step.getStatus());
        assertEquals("skipped", manifest.getStatus());
        assertFalse(manifest.getGenerated());
        // 验证：不调用任何 Provider 或 tools
        verifyNoInteractions(tools);
        verifyNoInteractions(assetService);
    }

    @Test
    void execute_IncludeMultimodalScriptNull_ReturnsSkipped() {
        context.setIncludeMultimodalScript(null);

        AgentStep step = multimodalAgent.execute(context, manifest, null);

        assertEquals(AgentStatus.SKIPPED, step.getStatus());
        verifyNoInteractions(tools);
    }

    @Test
    void execute_PodcastToggleOff_DoesNotCallPodcastProvider() {
        context.setIncludeMultimodalScript(true);
        context.setIncludePodcastAudio(false);  // 关闭播客
        context.setIncludeKnowledgeImages(false);  // 关闭图片

        when(tools.generateMultimodalScript(anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(Map.of("script", "多模态脚本"));

        AgentStep step = multimodalAgent.execute(context, manifest, null);

        assertEquals(AgentStatus.SUCCESS, step.getStatus());
        // 验证：不调用 generatePodcastScript
        verify(tools, never()).generatePodcastScript(any());
        // 验证：不调用 generateImagePrompts
        verify(tools, never()).generateImagePrompts(any());
        // 验证：不调用 assetService 生成播客
        verify(assetService, never()).generatePodcastAsset(anyLong(), anyString(), anyString(),
                anyLong(), anyString(), anyString(), anyString(), anyString());
        // 验证：不调用 assetService 生成图片
        verify(assetService, never()).generateImageAssets(anyLong(), anyString(), anyString(),
                anyLong(), any(), anyString(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void execute_ImageToggleOff_DoesNotCallImageProvider() {
        context.setIncludeMultimodalScript(true);
        context.setIncludePodcastAudio(true);
        context.setIncludeKnowledgeImages(false);  // 关闭图片

        when(tools.generateMultimodalScript(anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(Map.of("script", "多模态脚本"));
        when(tools.generatePodcastScript(any())).thenReturn("播客脚本");
        ResourceAssetVO podcastVO = new ResourceAssetVO();
        podcastVO.setStatus("success");
        podcastVO.setAssetType("audio");
        when(assetService.generatePodcastAsset(anyLong(), anyString(), anyString(),
                anyLong(), anyString(), any(), any(), anyString()))
                .thenReturn(podcastVO);

        AgentStep step = multimodalAgent.execute(context, manifest, null);

        assertEquals(AgentStatus.SUCCESS, step.getStatus());
        // 验证：不调用图片相关方法
        verify(tools, never()).generateImagePrompts(any());
        verify(assetService, never()).generateImageAssets(anyLong(), anyString(), anyString(),
                anyLong(), any(), anyString(), anyInt(), anyInt(), anyInt());
        // 验证：context.assets 中只有音频，没有图片
        assertEquals(1, context.getAssets().size());
        assertEquals("audio", context.getAssets().get(0).getAssetType());
    }

    // ============== 开关打开 - 写入 assets ==============

    @Test
    void execute_PodcastToggleOn_WritesAudioAssetToContext() {
        context.setIncludeMultimodalScript(true);
        context.setIncludePodcastAudio(true);
        context.setIncludeKnowledgeImages(false);
        context.setTtsVoice("xiaoyan");
        context.setPodcastStyle("teacher");

        when(tools.generateMultimodalScript(anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(Map.of("script", "脚本"));
        when(tools.generatePodcastScript(any())).thenReturn("这是播客脚本");
        ResourceAssetVO podcastVO = new ResourceAssetVO();
        podcastVO.setAssetId("asset-audio-1");
        podcastVO.setStatus("success");
        podcastVO.setAssetType("audio");
        podcastVO.setAssetRole("podcast");
        when(assetService.generatePodcastAsset(eq(1L), eq("pkg-1"), eq("task-1"),
                eq(100L), eq("这是播客脚本"), eq("xiaoyan"), eq("teacher"), anyString()))
                .thenReturn(podcastVO);

        AgentStep step = multimodalAgent.execute(context, manifest, null);

        assertEquals(AgentStatus.SUCCESS, step.getStatus());
        assertEquals(1, context.getAssets().size());
        ResourceAssetVO asset = context.getAssets().get(0);
        assertEquals("asset-audio-1", asset.getAssetId());
        assertEquals("audio", asset.getAssetType());
        assertEquals("podcast", asset.getAssetRole());
        assertEquals("success", asset.getStatus());
        // 验证 manifest 与 step 状态
        assertEquals("success", manifest.getStatus());
        assertTrue(manifest.getGenerated());
    }

    @Test
    void execute_ImageToggleOn_WritesImageAssetsToContext() {
        context.setIncludeMultimodalScript(true);
        context.setIncludePodcastAudio(false);
        context.setIncludeKnowledgeImages(true);
        context.setImageStyle("clean_edu");
        context.setImageCount(2);

        when(tools.generateMultimodalScript(anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(Map.of("script", "脚本"));

        ImagePromptPlan plan = new ImagePromptPlan();
        ImagePromptPlan.CoverPrompt cover = new ImagePromptPlan.CoverPrompt();
        cover.setTitle("封面");
        cover.setPrompt("封面提示词");
        plan.setCover(cover);
        ImagePromptPlan.ExplanationPrompt exp = new ImagePromptPlan.ExplanationPrompt();
        exp.setKnowledgePoint("知识点1");
        exp.setPrompt("提示词1");
        plan.setExplanations(List.of(exp));
        when(tools.generateImagePrompts(any())).thenReturn(plan);

        ResourceAssetVO coverVO = new ResourceAssetVO();
        coverVO.setAssetId("asset-cover");
        coverVO.setStatus("success");
        coverVO.setAssetType("image");
        coverVO.setAssetRole("cover");
        ResourceAssetVO expVO = new ResourceAssetVO();
        expVO.setAssetId("asset-exp");
        expVO.setStatus("success");
        expVO.setAssetType("image");
        expVO.setAssetRole("explanation");
        when(assetService.generateImageAssets(eq(1L), eq("pkg-1"), eq("task-1"),
                eq(100L), eq(plan), eq("clean_edu"), eq(2), eq(512), eq(512)))
                .thenReturn(List.of(coverVO, expVO));

        AgentStep step = multimodalAgent.execute(context, manifest, null);

        assertEquals(AgentStatus.SUCCESS, step.getStatus());
        assertEquals(2, context.getAssets().size());
        assertEquals("cover", context.getAssets().get(0).getAssetRole());
        assertEquals("explanation", context.getAssets().get(1).getAssetRole());
    }

    @Test
    void execute_BothTogglesOn_WritesBothAssetTypes() {
        context.setIncludeMultimodalScript(true);
        context.setIncludePodcastAudio(true);
        context.setIncludeKnowledgeImages(true);
        context.setTtsVoice("xiaoyan");
        context.setImageStyle("diagram");
        context.setImageCount(1);

        when(tools.generateMultimodalScript(anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(Map.of("script", "脚本"));
        when(tools.generatePodcastScript(any())).thenReturn("播客脚本");
        ImagePromptPlan plan = new ImagePromptPlan();
        ImagePromptPlan.CoverPrompt cover = new ImagePromptPlan.CoverPrompt();
        cover.setTitle("封面");
        cover.setPrompt("封面提示词");
        plan.setCover(cover);
        when(tools.generateImagePrompts(any())).thenReturn(plan);

        ResourceAssetVO audioVO = new ResourceAssetVO();
        audioVO.setAssetType("audio");
        audioVO.setStatus("success");
        when(assetService.generatePodcastAsset(anyLong(), anyString(), anyString(),
                anyLong(), anyString(), any(), any(), anyString())).thenReturn(audioVO);

        ResourceAssetVO coverVO = new ResourceAssetVO();
        coverVO.setAssetType("image");
        coverVO.setStatus("success");
        when(assetService.generateImageAssets(anyLong(), anyString(), anyString(),
                anyLong(), any(), anyString(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(coverVO));

        AgentStep step = multimodalAgent.execute(context, manifest, null);

        assertEquals(AgentStatus.SUCCESS, step.getStatus());
        assertEquals(2, context.getAssets().size());
        // 验证音频 + 图片都加入 assets
        assertTrue(context.getAssets().stream().anyMatch(a -> "audio".equals(a.getAssetType())));
        assertTrue(context.getAssets().stream().anyMatch(a -> "image".equals(a.getAssetType())));
    }

    // ============== Provider 失败不拖垮资源包 ==============

    @Test
    void execute_PodcastScriptGenerationFails_AddsFailedAsset_DoesNotCrash() {
        context.setIncludeMultimodalScript(true);
        context.setIncludePodcastAudio(true);
        context.setIncludeKnowledgeImages(false);

        when(tools.generateMultimodalScript(anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(Map.of("script", "脚本"));
        when(tools.generatePodcastScript(any())).thenThrow(new RuntimeException("LLM 调用失败"));

        AgentStep step = multimodalAgent.execute(context, manifest, null);

        // 验证：Agent 步骤仍为 SUCCESS（脚本生成成功，只是资产生成失败）
        assertEquals(AgentStatus.SUCCESS, step.getStatus());
        // 验证：assets 中有一个 failed 资产
        assertEquals(1, context.getAssets().size());
        ResourceAssetVO failedAsset = context.getAssets().get(0);
        assertEquals("failed", failedAsset.getStatus());
        assertEquals("audio", failedAsset.getAssetType());
        assertEquals("SCRIPT_ERROR", failedAsset.getErrorCode());
        // 验证：manifest 仍为 success
        assertEquals("success", manifest.getStatus());
        assertTrue(manifest.getGenerated());
        // 验证：outputSummary 说明部分失败
        assertNotNull(step.getOutputSummary());
        assertTrue(step.getOutputSummary().contains("失败"));
    }

    @Test
    void execute_PodcastProviderThrows_AddsFailedAsset_DoesNotCrash() {
        context.setIncludeMultimodalScript(true);
        context.setIncludePodcastAudio(true);
        context.setIncludeKnowledgeImages(false);

        when(tools.generateMultimodalScript(anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(Map.of("script", "脚本"));
        when(tools.generatePodcastScript(any())).thenReturn("播客脚本");
        when(assetService.generatePodcastAsset(anyLong(), anyString(), anyString(),
                anyLong(), anyString(), any(), any(), anyString()))
                .thenThrow(new RuntimeException("讯飞 TTS 网络异常"));

        AgentStep step = multimodalAgent.execute(context, manifest, null);

        assertEquals(AgentStatus.SUCCESS, step.getStatus());
        assertEquals(1, context.getAssets().size());
        ResourceAssetVO failedAsset = context.getAssets().get(0);
        assertEquals("failed", failedAsset.getStatus());
        assertEquals("PROVIDER_ERROR", failedAsset.getErrorCode());
    }

    @Test
    void execute_PodcastScriptEmpty_AddsFailedAsset() {
        context.setIncludeMultimodalScript(true);
        context.setIncludePodcastAudio(true);
        context.setIncludeKnowledgeImages(false);

        when(tools.generateMultimodalScript(anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(Map.of("script", "脚本"));
        when(tools.generatePodcastScript(any())).thenReturn("");  // 空脚本

        AgentStep step = multimodalAgent.execute(context, manifest, null);

        assertEquals(AgentStatus.SUCCESS, step.getStatus());
        assertEquals(1, context.getAssets().size());
        assertEquals("failed", context.getAssets().get(0).getStatus());
        assertEquals("EMPTY_SCRIPT", context.getAssets().get(0).getErrorCode());
    }

    @Test
    void execute_ImagePromptGenerationFails_AddsFailedAsset_DoesNotCrash() {
        context.setIncludeMultimodalScript(true);
        context.setIncludePodcastAudio(false);
        context.setIncludeKnowledgeImages(true);

        when(tools.generateMultimodalScript(anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(Map.of("script", "脚本"));
        when(tools.generateImagePrompts(any())).thenThrow(new RuntimeException("LLM 调用失败"));

        AgentStep step = multimodalAgent.execute(context, manifest, null);

        assertEquals(AgentStatus.SUCCESS, step.getStatus());
        assertEquals(1, context.getAssets().size());
        assertEquals("failed", context.getAssets().get(0).getStatus());
        assertEquals("PROMPT_ERROR", context.getAssets().get(0).getErrorCode());
        // 验证：不调用 assetService
        verify(assetService, never()).generateImageAssets(anyLong(), anyString(), anyString(),
                anyLong(), any(), anyString(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void execute_ImagePromptNull_AddsFailedAsset() {
        context.setIncludeMultimodalScript(true);
        context.setIncludePodcastAudio(false);
        context.setIncludeKnowledgeImages(true);

        when(tools.generateMultimodalScript(anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(Map.of("script", "脚本"));
        when(tools.generateImagePrompts(any())).thenReturn(null);

        AgentStep step = multimodalAgent.execute(context, manifest, null);

        assertEquals(AgentStatus.SUCCESS, step.getStatus());
        assertEquals(1, context.getAssets().size());
        assertEquals("failed", context.getAssets().get(0).getStatus());
        assertEquals("EMPTY_PROMPT", context.getAssets().get(0).getErrorCode());
    }

    @Test
    void execute_ImageProviderThrows_AddsFailedAsset_DoesNotCrash() {
        context.setIncludeMultimodalScript(true);
        context.setIncludePodcastAudio(false);
        context.setIncludeKnowledgeImages(true);

        when(tools.generateMultimodalScript(anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(Map.of("script", "脚本"));
        ImagePromptPlan plan = new ImagePromptPlan();
        ImagePromptPlan.CoverPrompt cover = new ImagePromptPlan.CoverPrompt();
        cover.setTitle("封面");
        cover.setPrompt("提示词");
        plan.setCover(cover);
        when(tools.generateImagePrompts(any())).thenReturn(plan);
        when(assetService.generateImageAssets(anyLong(), anyString(), anyString(),
                anyLong(), any(), anyString(), anyInt(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("讯飞文生图异常"));

        AgentStep step = multimodalAgent.execute(context, manifest, null);

        assertEquals(AgentStatus.SUCCESS, step.getStatus());
        assertEquals(1, context.getAssets().size());
        assertEquals("failed", context.getAssets().get(0).getStatus());
        assertEquals("PROVIDER_ERROR", context.getAssets().get(0).getErrorCode());
    }

    @Test
    void execute_AllAssetsFailed_StepSuccessWithFailureSummary() {
        context.setIncludeMultimodalScript(true);
        context.setIncludePodcastAudio(true);
        context.setIncludeKnowledgeImages(true);
        context.setImageCount(1);

        when(tools.generateMultimodalScript(anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(Map.of("script", "脚本"));
        when(tools.generatePodcastScript(any())).thenThrow(new RuntimeException("脚本失败"));
        when(tools.generateImagePrompts(any())).thenThrow(new RuntimeException("提示词失败"));

        AgentStep step = multimodalAgent.execute(context, manifest, null);

        assertEquals(AgentStatus.SUCCESS, step.getStatus());
        // 验证：两个 failed 资产
        assertEquals(2, context.getAssets().size());
        assertTrue(context.getAssets().stream().allMatch(a -> "failed".equals(a.getStatus())));
        // 验证：outputSummary 说明全部失败
        assertTrue(step.getOutputSummary().contains("全部失败"));
    }

    @Test
    void execute_PartialAssetFailure_StepSuccessWithPartialSummary() {
        context.setIncludeMultimodalScript(true);
        context.setIncludePodcastAudio(true);
        context.setIncludeKnowledgeImages(true);
        context.setImageCount(1);

        when(tools.generateMultimodalScript(anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(Map.of("script", "脚本"));
        // 播客成功
        when(tools.generatePodcastScript(any())).thenReturn("播客脚本");
        ResourceAssetVO audioOK = new ResourceAssetVO();
        audioOK.setAssetType("audio");
        audioOK.setStatus("success");
        when(assetService.generatePodcastAsset(anyLong(), anyString(), anyString(),
                anyLong(), anyString(), any(), any(), anyString())).thenReturn(audioOK);
        // 图片失败
        when(tools.generateImagePrompts(any())).thenThrow(new RuntimeException("失败"));

        AgentStep step = multimodalAgent.execute(context, manifest, null);

        assertEquals(AgentStatus.SUCCESS, step.getStatus());
        assertEquals(2, context.getAssets().size());
        // 验证：1 个 success + 1 个 failed
        long successCount = context.getAssets().stream()
                .filter(a -> "success".equals(a.getStatus())).count();
        long failedCount = context.getAssets().stream()
                .filter(a -> "failed".equals(a.getStatus())).count();
        assertEquals(1, successCount);
        assertEquals(1, failedCount);
        // 验证：outputSummary 说明部分失败
        assertTrue(step.getOutputSummary().contains("部分资产失败"));
    }

    // ============== 进度上报 ==============

    @Test
    void execute_BothTogglesOn_ReportsExpectedProgressMilestones() {
        context.setIncludeMultimodalScript(true);
        context.setIncludePodcastAudio(true);
        context.setIncludeKnowledgeImages(true);
        context.setImageCount(1);

        when(tools.generateMultimodalScript(anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(Map.of("script", "脚本"));
        when(tools.generatePodcastScript(any())).thenReturn("脚本");
        when(tools.generateImagePrompts(any())).thenReturn(new ImagePromptPlan());

        ResourceAssetVO audioOK = new ResourceAssetVO();
        audioOK.setStatus("success");
        when(assetService.generatePodcastAsset(anyLong(), anyString(), anyString(),
                anyLong(), anyString(), any(), any(), anyString())).thenReturn(audioOK);
        when(assetService.generateImageAssets(anyLong(), anyString(), anyString(),
                anyLong(), any(), anyString(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of());

        java.util.List<Integer> progresses = new java.util.ArrayList<>();
        Consumer<com.study.ai.agent.core.AgentProgressEvent> reporter = event ->
                progresses.add(event.progress());

        multimodalAgent.execute(context, manifest, reporter);

        // 验证：上报了 84, 87, 90, 93, 94 这几个进度
        assertTrue(progresses.contains(84), "应上报进度 84（生成多模态脚本）");
        assertTrue(progresses.contains(87), "应上报进度 87（生成播客讲稿）");
        assertTrue(progresses.contains(90), "应上报进度 90（合成音频）");
        assertTrue(progresses.contains(93), "应上报进度 93（生成知识配图）");
        assertTrue(progresses.contains(94), "应上报进度 94（保存多模态资源）");
    }

    @Test
    void execute_NullProgressReporter_DoesNotThrow() {
        context.setIncludeMultimodalScript(true);
        context.setIncludePodcastAudio(false);
        context.setIncludeKnowledgeImages(false);

        when(tools.generateMultimodalScript(anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(Map.of("script", "脚本"));

        // 不应抛出 NPE
        assertDoesNotThrow(() -> multimodalAgent.execute(context, manifest, null));
    }

    // ============== 异常 - 脚本生成失败 ==============

    @Test
    void execute_MultimodalScriptGenerationThrows_StepFailed() {
        context.setIncludeMultimodalScript(true);
        context.setIncludePodcastAudio(false);
        context.setIncludeKnowledgeImages(false);

        when(tools.generateMultimodalScript(anyString(), anyString(), anyString(), anyLong()))
                .thenThrow(new RuntimeException("脚本生成失败"));

        AgentStep step = multimodalAgent.execute(context, manifest, null);

        // 脚本生成失败时，step 状态为 FAILED
        assertEquals(AgentStatus.FAILED, step.getStatus());
        assertEquals("failed", manifest.getStatus());
        assertFalse(manifest.getGenerated());
        assertEquals("MULTIMODAL_ERROR", step.getErrorCode());
    }
}
