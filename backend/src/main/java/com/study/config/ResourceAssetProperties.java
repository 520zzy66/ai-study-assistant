package com.study.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 资源资产存储与下载配置。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "resource-asset")
public class ResourceAssetProperties {

    /** 资产文件存储根目录（相对路径，禁止暴露绝对路径到前端） */
    private String storageDir;

    /** 公网下载/预览 URL 前缀，前端拼接 assetId 使用 */
    private String publicUrlPrefix = "/api/ai/resource-assets";

    /** 单个音频资产最大字节数（默认 20MB） */
    private long maxAudioBytes = 20L * 1024 * 1024;

    /** 单个图片资产最大字节数（默认 10MB） */
    private long maxImageBytes = 10L * 1024 * 1024;
}
