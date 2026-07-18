package com.study.ai.agent.multimodal;

import com.study.ai.agent.core.AgentNames;
import com.study.ai.agent.core.AgentProgressEvent;
import com.study.ai.agent.core.AgentStatus;
import com.study.ai.agent.core.AgentStep;
import com.study.ai.agent.resource.ResourceAgentContext;
import com.study.ai.agent.resource.ResourceManifestItem;
import com.study.ai.agent.tool.ResourceGenerationTools;
import com.study.ai.multimodal.model.ImagePromptPlan;
import com.study.ai.multimodal.service.ResourceAssetService;
import com.study.config.XfyunProperties;
import com.study.dto.vo.ResourceAssetVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 多模态资源 Agent。
 *
 * <p>spec §8.4 升级：从"脚本生成 Agent"升级为"脚本 + 资产生成 Agent"。
 *
 * <p>进度编排（spec §4.2）：
 * <ul>
 *   <li>84：正在整理多模态脚本（生成多模态脚本包，保持原逻辑）</li>
 *   <li>87：正在生成播客讲稿（includePodcastAudio=true 时）</li>
 *   <li>90：正在合成音频解说（includePodcastAudio=true 时）</li>
 *   <li>93：正在生成知识配图（includeKnowledgeImages=true 时）</li>
 *   <li>94：正在保存多模态资源（汇总）</li>
 * </ul>
 *
 * <p>失败容忍：
 * <ul>
 *   <li>Provider 未启用时，生成 failed 资产记录（errorCode=CONFIG_MISSING），
 *       AgentStep 状态为 SUCCESS，但 outputSummary 说明部分失败。</li>
 *   <li>音频/图片生成全部失败时不影响总结、题库、学习路径等主资源，
 *       SafetyAgent 在 qualityChecks 中追加说明。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MultimodalAgent {
    private final ResourceGenerationTools tools;
    private final ResourceAssetService assetService;
    private final XfyunProperties xfyunProperties;

    /**
     * 生成多模态脚本包与音频/图片资产。
     *
     * @param context shared resource agent context
     * @param manifest resource manifest item to update
     * @param progressReporter progress callback
     * @return trace step for this agent
     */
    public AgentStep execute(ResourceAgentContext context, ResourceManifestItem manifest,
                              Consumer<AgentProgressEvent> progressReporter) {
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

        boolean podcastEnabled = Boolean.TRUE.equals(context.getIncludePodcastAudio());
        boolean imageEnabled = Boolean.TRUE.equals(context.getIncludeKnowledgeImages());

        try {
            // 进度 84：生成多模态脚本（保持原逻辑）
            report(progressReporter, 84, "正在整理多模态脚本");
            Object script = tools.generateMultimodalScript(context.getGoal(), context.getProfileText(),
                    context.getSummary(), context.getMaterialId());
            Map<String, Object> resource = Map.of(
                    "type", "multimodal_script",
                    "title", "多模态资源",
                    "content", script
            );
            context.getResources().put("multimodalScript", resource);

            // 进度 87 + 90：播客音频
            if (podcastEnabled) {
                generatePodcastAsset(context, progressReporter);
            }

            // 进度 93：知识配图
            if (imageEnabled) {
                generateImageAssets(context, progressReporter);
            }

            // 进度 94：保存多模态资源（汇总）
            report(progressReporter, 94, "正在保存多模态资源");

            // 设置 manifest 与 step 状态
            boolean anyAssetFailed = context.getAssets().stream()
                    .anyMatch(a -> "failed".equals(a.getStatus()));
            boolean anyAssetSuccess = context.getAssets().stream()
                    .anyMatch(a -> "success".equals(a.getStatus()));
            boolean hasAssets = !context.getAssets().isEmpty();

            manifest.setStatus("success");
            manifest.setGenerated(true);
            step.setStatus(AgentStatus.SUCCESS);
            if (hasAssets && anyAssetFailed && !anyAssetSuccess) {
                step.setOutputSummary("多模态脚本生成成功，但音频/图片资产全部失败");
            } else if (hasAssets && anyAssetFailed) {
                step.setOutputSummary("多模态脚本生成成功，部分资产失败");
            } else if (hasAssets) {
                step.setOutputSummary("成功生成多模态脚本与资产");
            } else {
                step.setOutputSummary("成功生成多模态脚本");
            }
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

    /**
     * 生成播客音频资产（进度 87 → 90）。
     */
    private void generatePodcastAsset(ResourceAgentContext context, Consumer<AgentProgressEvent> progressReporter) {
        report(progressReporter, 87, "正在生成播客讲稿");
        String script;
        try {
            script = tools.generatePodcastScript(context);
        } catch (Exception e) {
            log.warn("生成播客脚本失败: materialId={}", context.getMaterialId(), e);
            // 直接生成一个 failed 资产记录，不阻断主流程
            ResourceAssetVO failed = buildFailedPodcast(context, "SCRIPT_ERROR", "生成播客脚本失败");
            context.getAssets().add(failed);
            return;
        }
        if (script == null || script.isBlank()) {
            log.warn("播客脚本为空: materialId={}", context.getMaterialId());
            ResourceAssetVO failed = buildFailedPodcast(context, "EMPTY_SCRIPT", "播客脚本为空");
            context.getAssets().add(failed);
            return;
        }

        report(progressReporter, 90, "正在合成音频解说");
        try {
            ResourceAssetVO vo = assetService.generatePodcastAsset(
                    context.getUserId(),
                    context.getPackageId(),
                    context.getTaskId(),
                    context.getMaterialId(),
                    script,
                    context.getTtsVoice(),
                    context.getPodcastStyle(),
                    "summary,mindMap,quiz,plan,multimodalScript");
            context.getAssets().add(vo);
        } catch (Exception e) {
            log.warn("播客资产生成异常: materialId={}", context.getMaterialId(), e);
            ResourceAssetVO failed = buildFailedPodcast(context, "PROVIDER_ERROR", "音频合成调用异常");
            context.getAssets().add(failed);
        }
    }

    /**
     * 生成知识配图资产（进度 93）。
     */
    private void generateImageAssets(ResourceAgentContext context, Consumer<AgentProgressEvent> progressReporter) {
        report(progressReporter, 93, "正在生成知识配图");
        ImagePromptPlan promptPlan;
        try {
            promptPlan = tools.generateImagePrompts(context);
        } catch (Exception e) {
            log.warn("生成图片提示词失败: materialId={}", context.getMaterialId(), e);
            ResourceAssetVO failed = buildFailedImage(context, "cover", "PROMPT_ERROR", "生成图片提示词失败");
            context.getAssets().add(failed);
            return;
        }
        if (promptPlan == null) {
            ResourceAssetVO failed = buildFailedImage(context, "cover", "EMPTY_PROMPT", "图片提示词为空");
            context.getAssets().add(failed);
            return;
        }

        int imageCount = context.getImageCount() != null ? context.getImageCount() : 1;
        int width = parseDefaultWidth();
        int height = parseDefaultHeight();
        try {
            List<ResourceAssetVO> imageAssets = assetService.generateImageAssets(
                    context.getUserId(),
                    context.getPackageId(),
                    context.getTaskId(),
                    context.getMaterialId(),
                    promptPlan,
                    context.getImageStyle(),
                    imageCount,
                    width,
                    height);
            if (imageAssets != null) {
                context.getAssets().addAll(imageAssets);
            }
        } catch (Exception e) {
            log.warn("图片资产生成异常: materialId={}", context.getMaterialId(), e);
            ResourceAssetVO failed = buildFailedImage(context, "cover", "PROVIDER_ERROR", "图片生成调用异常");
            context.getAssets().add(failed);
        }
    }

    /**
     * 构造一个 failed 状态的播客资产 VO（用于脚本生成失败或调用异常时）。
     */
    private ResourceAssetVO buildFailedPodcast(ResourceAgentContext context, String errorCode, String errorMessage) {
        ResourceAssetVO vo = new ResourceAssetVO();
        vo.setAssetId("asset_failed_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        vo.setPackageId(context.getPackageId());
        vo.setTaskId(context.getTaskId());
        vo.setMaterialId(context.getMaterialId());
        vo.setAssetType("audio");
        vo.setAssetRole("podcast");
        vo.setStatus("failed");
        vo.setProvider(ResourceAssetService.PROVIDER_XFYUN_TTS);
        vo.setErrorCode(errorCode);
        vo.setErrorMessage(errorMessage);
        return vo;
    }

    /**
     * 构造一个 failed 状态的图片资产 VO（用于提示词生成失败或调用异常时）。
     */
    private ResourceAssetVO buildFailedImage(ResourceAgentContext context, String role, String errorCode, String errorMessage) {
        ResourceAssetVO vo = new ResourceAssetVO();
        vo.setAssetId("asset_failed_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        vo.setPackageId(context.getPackageId());
        vo.setTaskId(context.getTaskId());
        vo.setMaterialId(context.getMaterialId());
        vo.setAssetType("image");
        vo.setAssetRole(role);
        vo.setStatus("failed");
        vo.setProvider(ResourceAssetService.PROVIDER_XFYUN_TTI);
        vo.setErrorCode(errorCode);
        vo.setErrorMessage(errorMessage);
        return vo;
    }

    /**
     * 解析默认图片宽度。
     */
    private int parseDefaultWidth() {
        try {
            String size = xfyunProperties.getImage().getDefaultSize();
            if (size != null && size.contains("x")) {
                return Integer.parseInt(size.split("x")[0]);
            }
        } catch (Exception ignored) {
            // ignore
        }
        return 512;
    }

    /**
     * 解析默认图片高度。
     */
    private int parseDefaultHeight() {
        try {
            String size = xfyunProperties.getImage().getDefaultSize();
            if (size != null && size.contains("x")) {
                return Integer.parseInt(size.split("x")[1]);
            }
        } catch (Exception ignored) {
            // ignore
        }
        return 512;
    }

    /**
     * 上报进度。
     */
    private void report(Consumer<AgentProgressEvent> reporter, int progress, String message) {
        if (reporter != null) {
            reporter.accept(new AgentProgressEvent(progress, AgentNames.MULTIMODAL_AGENT,
                    AgentNames.ACTION_GENERATE_MULTIMODAL_SCRIPT, message));
        }
    }
}
