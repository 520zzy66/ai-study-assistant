package com.study.ai.multimodal.provider;

import com.study.ai.multimodal.model.AssetGenerationResult;
import com.study.ai.multimodal.model.ImageGenerationRequest;

import java.util.List;

/**
 * 文生图 Provider 接口。
 *
 * <p>抽象不同的图片生成提供方（如讯飞星火文生图、HiDream、未来其他厂商），
 * 业务层通过此接口调用，不直接依赖具体实现，便于后续替换或扩展。
 */
public interface ImageGenerationProvider {

    /**
     * 根据提示词生成一张或多张图片资产。
     *
     * @param request 图片生成请求，包含提示词、风格、尺寸、角色等
     * @return 生成结果列表，每张图一个结果；失败时返回包含失败信息的结果
     */
    List<AssetGenerationResult> generate(ImageGenerationRequest request);

    /**
     * 判断 Provider 配置是否完整且已启用。
     *
     * @return true 表示当前可用；false 表示未配置或被禁用
     */
    boolean isAvailable();
}
