package com.study.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 科大讯飞能力配置。
 *
 * <p>密钥只允许通过环境变量注入，日志中禁止打印鉴权 URL、签名原文和完整错误响应。
 * 当 {@code enabled=true} 但密钥不完整时，应用启动不失败，能力接口返回 disabled。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "xfyun")
public class XfyunProperties {

    /** 讯飞 APPID */
    private String appId;

    /** 讯飞 APIKey */
    private String apiKey;

    /** 讯飞 APISecret */
    private String apiSecret;

    /** 在线语音合成配置 */
    private Tts tts = new Tts();

    /** 文生图配置 */
    private Image image = new Image();

    /**
     * 在线 TTS 配置。
     */
    @Data
    public static class Tts {
        private String appId;

        private String apiKey;

        private String apiSecret;

        /** 是否启用 TTS 能力 */
        private boolean enabled = false;

        /** WebSocket 端点 */
        private String endpoint = "wss://tts-api.xfyun.cn/v2/tts";

        /** 默认发音人 */
        private String voice = "xiaoyan";

        /** 采样率 */
        private int sampleRate = 16000;

        /** 音频格式：mp3 */
        private String format = "mp3";

        /** 单次合成超时（毫秒） */
        private long timeoutMs = 60000L;

        /** 单次请求文本最大字节数（小于官方 8000 字节上限） */
        private int maxTextBytes = 7600;
    }

    /**
     * 文生图配置。
     */
    @Data
    public static class Image {
        private String appId;

        private String apiKey;

        private String apiSecret;

        /** 是否启用文生图能力 */
        private boolean enabled = false;

        /** 图片 Provider：tti / hidream */
        private String provider = "hidream";

        /** 文生图端点；HiDream 模式下为任务创建端点 */
        private String endpoint = "https://cn-huadong-1.xf-yun.com/v1/private/s3fd61810/create";

        /** HiDream 任务查询端点；为空时由创建端点自动推导 */
        private String hidreamQueryEndpoint;

        /** HiDream 轮询间隔（毫秒） */
        private long hidreamPollIntervalMs = 1500L;

        /** 模型标识 */
        private String model = "HiDream";

        /** 单次生成超时（毫秒） */
        private long timeoutMs = 60000L;

        /** 单次任务最大图片数 */
        private int maxImageCount = 4;

        /** 默认分辨率 */
        private String defaultSize = "1024x1024";
    }

    /**
     * 判断 TTS 配置是否完整可用。
     *
     * @return true 表示 APPID/APIKey/APISecret 均非空且 enabled=true
     */
    public boolean isTtsConfigured() {
        return tts.enabled
                && isPresent(getTtsAppId())
                && isPresent(getTtsApiKey())
                && isPresent(getTtsApiSecret());
    }

    /**
     * 判断文生图配置是否完整可用。
     *
     * @return true 表示 APPID/APIKey/APISecret 均非空且 enabled=true
     */
    public boolean isImageConfigured() {
        return image.enabled
                && isPresent(getImageAppId())
                && isPresent(getImageApiKey())
                && isPresent(getImageApiSecret());
    }

    /**
     * Returns the APPID used by TTS calls, with legacy common config as fallback.
     */
    public String getTtsAppId() {
        return firstPresent(tts.appId, appId);
    }

    /**
     * Returns the APIKey used by TTS calls, with legacy common config as fallback.
     */
    public String getTtsApiKey() {
        return firstPresent(tts.apiKey, apiKey);
    }

    /**
     * Returns the APISecret used by TTS calls, with legacy common config as fallback.
     */
    public String getTtsApiSecret() {
        return firstPresent(tts.apiSecret, apiSecret);
    }

    /**
     * Returns the APPID used by image generation calls, with legacy common config as fallback.
     */
    public String getImageAppId() {
        return firstPresent(image.appId, appId);
    }

    /**
     * Returns the APIKey used by image generation calls, with legacy common config as fallback.
     */
    public String getImageApiKey() {
        return firstPresent(image.apiKey, apiKey);
    }

    /**
     * Returns the APISecret used by image generation calls, with legacy common config as fallback.
     */
    public String getImageApiSecret() {
        return firstPresent(image.apiSecret, apiSecret);
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private static String firstPresent(String primary, String fallback) {
        return isPresent(primary) ? primary : fallback;
    }
}
