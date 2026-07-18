package com.study.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 个性化资源包生成请求。
 *
 * <p>多模态拓展字段（spec §8.1）：默认不启用，旧请求保持完全兼容。
 */
@Data
public class GenerateResourcePackageRequest {

    /** 资料 ID，第一版仅支持单资料生成。 */
    @NotNull(message = "资料ID不能为空")
    private Long materialId;

    /** 学习目标，不填时根据资料名和学习画像自动推断。 */
    @Size(max = 200, message = "学习目标最多200字")
    private String goal;

    /** 截止日期 yyyy-MM-dd，不填时默认生成 14 天学习路径。 */
    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "日期格式必须为 yyyy-MM-dd")
    private String examDate;

    /** 每日学习时长，单位小时。 */
    @Min(1)
    @Max(12)
    private Integer dailyHours;

    /** 难度 easy/medium/hard，不填时根据学习画像映射。 */
    @Pattern(regexp = "^(easy|medium|hard)?$", message = "难度必须是 easy、medium 或 hard")
    private String difficulty;

    /** 是否生成讲解文档。 */
    private Boolean includeSummary;

    /** 是否生成思维导图。 */
    private Boolean includeMindMap;

    /** 是否生成题库。 */
    private Boolean includeQuiz;

    /** 是否生成学习路径。 */
    private Boolean includePlan;

    /** 是否生成多模态脚本包。 */
    private Boolean includeMultimodalScript;

    // ============ 多模态拓展字段（spec §8.1） ============

    /** 是否生成播客音频解说。null 视为 false。 */
    private Boolean includePodcastAudio;

    /** 播客风格：teacher / review / story。null 视为 teacher。 */
    @Pattern(regexp = "^(teacher|review|story)?$", message = "播客风格必须是 teacher、review 或 story")
    private String podcastStyle;

    /** 发音人 ID，null 使用后端默认声音。具体可选值由能力接口返回。 */
    @Size(max = 64, message = "发音人ID最多64字符")
    private String ttsVoice;

    /** 是否生成知识配图。null 视为 false。 */
    private Boolean includeKnowledgeImages;

    /** 图片数量（含封面），范围 1 到 4。null 视为 1。 */
    @Min(value = 1, message = "图片数量最少1张")
    @Max(value = 4, message = "图片数量最多4张")
    private Integer imageCount;

    /** 图片风格：clean_edu / diagram / blackboard / isometric。null 视为 clean_edu。 */
    @Pattern(regexp = "^(clean_edu|diagram|blackboard|isometric)?$",
            message = "图片风格必须是 clean_edu、diagram、blackboard 或 isometric")
    private String imageStyle;
}
