package com.study.dto.vo;

import lombok.Data;

import java.util.List;

/**
 * 资源资产能力视图对象。
 *
 * <p>由 {@code GET /api/ai/resource-assets/capabilities} 返回，前端据此控制
 * 多模态拓展开关的可用性和默认值，不读取任何配置文件或密钥。
 */
@Data
public class ResourceAssetCapabilitiesVO {

    /** 是否启用讯飞 TTS */
    private Boolean ttsEnabled;

    /** 是否启用讯飞文生图 */
    private Boolean imageEnabled;

    /** 可选发音人列表 */
    private List<VoiceOption> voices;

    /** 可选图片风格列表 */
    private List<String> imageStyles;

    /** 单次任务最大图片数 */
    private Integer maxImageCount;

    /** 默认发音人 ID */
    private String defaultVoice;

    /** 默认图片风格 */
    private String defaultImageStyle;
}
