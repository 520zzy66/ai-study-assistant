package com.study.ai.agent.resource;

import com.study.dto.vo.ResourceAssetVO;
import com.study.entity.LearningMaterial;
import com.study.vo.UserProfileVO;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 资源工坊任务专属 Agent 上下文
 */
@Data
public class ResourceAgentContext {
    private Long userId;
    private Long materialId;
    private String goal;
    private String examDate;
    private Integer dailyHours;
    private String difficulty;
    private Boolean includeSummary;
    private Boolean includeMindMap;
    private Boolean includeQuiz;
    private Boolean includePlan;
    private Boolean includeMultimodalScript;

    /** 资源包 ID（Orchestrator 生成后写入，MultimodalAgent 写资产时使用）。 */
    private String packageId;

    /** 异步任务 ID（可空，写入 resource_asset.task_id）。 */
    private String taskId;

    // ============ 多模态拓展字段（spec §8.2） ============

    /** 是否生成播客音频解说。 */
    private Boolean includePodcastAudio;

    /** 播客风格：teacher / review / story。 */
    private String podcastStyle;

    /** 发音人 ID，为空时使用后端默认。 */
    private String ttsVoice;

    /** 是否生成知识配图。 */
    private Boolean includeKnowledgeImages;

    /** 图片数量（含封面）。 */
    private Integer imageCount;

    /** 图片风格。 */
    private String imageStyle;

    /** 已生成的多模态资产列表（MultimodalAgent 写入，SafetyAgent / 前端读取）。 */
    private List<ResourceAssetVO> assets = new ArrayList<>();

    private UserProfileVO profile;
    private LearningMaterial material;
    private String profileText;
    private String summary;

    // 用于存放各 Agent 生成并互相共享的资源
    private Map<String, Object> resources = new LinkedHashMap<>();
}
