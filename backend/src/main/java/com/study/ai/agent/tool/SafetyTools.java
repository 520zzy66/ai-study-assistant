package com.study.ai.agent.tool;

import com.study.common.BusinessException;
import com.study.dto.request.GenerateResourcePackageRequest;
import com.study.entity.LearningMaterial;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class SafetyTools {

    /**
     * Validates that at least one resource type is selected.
     *
     * @param request resource package request
     */
    public void validateSelectedResources(GenerateResourcePackageRequest request) {
        if (!enabled(request.getIncludeSummary())
                && !enabled(request.getIncludeMindMap())
                && !enabled(request.getIncludeQuiz())
                && !enabled(request.getIncludePlan())
                && !enabled(request.getIncludeMultimodalScript())) {
            throw new BusinessException(400, "至少选择一种资源类型");
        }
    }

    private boolean enabled(Boolean value) {
        return value == null || Boolean.TRUE.equals(value);
    }

    /**
     * Builds deterministic quality and safety notes for the generated package.
     *
     * @param material material entity
     * @param resources generated resource map
     * @return quality check map
     */
    public Map<String, Object> buildQualityChecks(LearningMaterial material, Map<String, Object> resources) {
        return Map.of(
                "materialReady", true,
                "materialId", material.getId(),
                "grounding", "资源生成基于当前用户已处理完成的学习资料，并复用现有总结、导图、题库和学习计划能力",
                "resourceCount", resources.size(),
                "multimodalMode", resources.containsKey("multimodalScript") ? "script_package" : "not_requested",
                "safety", "当前版本完成资料归属校验、参数校验和结构化输出约束；讯飞文本合规 Provider 作为后续增强接入"
        );
    }

    /**
     * Builds presentation notes for competition demos and exports.
     *
     * @param resources generated resource map
     * @return presentation note list
     */
    public List<String> buildPresentationNotes(Map<String, Object> resources) {
        List<String> notes = new ArrayList<>();
        notes.add("通过资源工坊把单份高校课程资料编排为可学习、可练习、可展示的个性化资源包。");
        notes.add("页面展示多智能体协作链路，任务轮询展示阶段进度，避免长时间白屏等待。");
        if (resources.containsKey("multimodalScript")) {
            notes.add("多模态脚本包可作为后续接入讯飞智能 PPT、图片生成、语音合成和视频生成的中间层。");
        }
        notes.add("支持历史资源包恢复与 Markdown/JSON 导出，便于初赛文档、PPT 和演示视频制作。");
        return notes;
    }
}
