package com.study.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI Chat 配置属性
 * 绑定 application.yml 中的 ai.chat.* 配置项
 *
 * <p>注意：超时配置已迁移至 Spring AI 原生配置 spring.ai.openai.client.read-timeout，
 * 不再通过此 Bean 管理。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.chat")
public class AiChatProperties {

    /** 模型名称，默认为 deepseek-chat */
    private String model = "deepseek-chat";

    /** 温度参数（0.0~2.0），默认 0.7 */
    private Double temperature = 0.7;
}
