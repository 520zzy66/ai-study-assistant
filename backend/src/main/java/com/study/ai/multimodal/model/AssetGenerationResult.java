package com.study.ai.multimodal.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Provider 资产生成结果。
 *
 * <p>音频 Provider 返回单个结果；图片 Provider 一次可返回多个结果（每张图一个）。
 * 失败时 {@code success=false}，错误信息通过 {@code errorType}/{@code errorCode}/
 * {@code errorMessage} 暴露给业务层，原始远端错误不外泄。
 */
@Data
@Builder
public class AssetGenerationResult {

    /** 是否成功 */
    private boolean success;

    /** 生成内容字节（成功时必填） */
    private byte[] content;

    /** Content-Type，如 audio/mpeg、image/png */
    private String contentType;

    /** 提供方标识，如 xfyun-tts / xfyun-tti */
    private String provider;

    /** 模型标识 */
    private String model;

    /** 文件扩展名（不含点），如 mp3 / png */
    private String fileExtension;

    /** 图片宽度（仅图片） */
    private Integer width;

    /** 图片高度（仅图片） */
    private Integer height;

    /** 音频时长（秒，仅音频） */
    private Integer durationSeconds;

    /** 失败错误码 */
    private String errorCode;

    /** 失败原因（业务可理解） */
    private String errorMessage;

    /** 失败错误类型 */
    private ProviderErrorType errorType;

    /** 扩展元数据 */
    private Map<String, Object> metadata;

    /**
     * 构造失败结果。
     *
     * @param provider 提供方
     * @param errorType 错误类型
     * @param errorCode 错误码
     * @param errorMessage 错误消息
     * @return 失败结果
     */
    public static AssetGenerationResult failure(String provider, ProviderErrorType errorType,
                                                 String errorCode, String errorMessage) {
        return AssetGenerationResult.builder()
                .success(false)
                .provider(provider)
                .errorType(errorType)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
}
