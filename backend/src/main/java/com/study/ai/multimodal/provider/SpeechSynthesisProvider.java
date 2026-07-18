package com.study.ai.multimodal.provider;

import com.study.ai.multimodal.model.AssetGenerationResult;
import com.study.ai.multimodal.model.AudioSynthesisRequest;

/**
 * 语音合成 Provider 接口。
 *
 * <p>抽象不同的 TTS 提供方（如讯飞在线 TTS、未来其他厂商），
 * 业务层通过此接口调用，不直接依赖具体实现，便于后续替换或扩展。
 */
public interface SpeechSynthesisProvider {

    /**
     * 将提供的口播文本合成为音频资产。
     *
     * @param request 语音合成请求，包含文本、发音人、播客风格等
     * @return 合成结果，成功时包含音频字节，失败时包含错误信息
     */
    AssetGenerationResult synthesize(AudioSynthesisRequest request);

    /**
     * 判断 Provider 配置是否完整且已启用。
     *
     * @return true 表示当前可用；false 表示未配置或被禁用
     */
    boolean isAvailable();
}
