package com.study.ai.multimodal.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文生图请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ImageGenerationRequest extends AssetGenerationRequest {

    /** 图片提示词（已合成风格描述） */
    private String prompt;

    /** 反向提示词 */
    private String negativePrompt;

    /** 图片风格：clean_edu / diagram / blackboard / isometric */
    private String imageStyle;

    /** 目标宽度 */
    private Integer width;

    /** 目标高度 */
    private Integer height;

    /** 资产角色：cover / explanation */
    private String assetRole;

    /** 知识点标题（explanation 角色使用，封面为空） */
    private String knowledgePoint;
}
