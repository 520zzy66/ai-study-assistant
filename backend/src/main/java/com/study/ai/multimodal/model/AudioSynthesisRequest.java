package com.study.ai.multimodal.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 语音合成请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AudioSynthesisRequest extends AssetGenerationRequest {

    /** 待合成文本（已清洗，去除 Markdown） */
    private String text;

    /** 发音人 ID，为空时使用后端默认 */
    private String voice;

    /** 播客风格：teacher / review / story */
    private String podcastStyle;

    /** 来源资源 key 列表（逗号分隔，如 summary,mindMap,quiz） */
    private String sourceKeys;
}
