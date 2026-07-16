package com.study.config;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 全局配置
 * 增强对 AI 返回的不规范 JSON 的包容性
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customJackson() {
        return builder -> {
            // 允许解析包含未转义控制字符（如 \n、\t）的 JSON 字符串
            builder.featuresToEnable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature());
            // 允许使用单引号
            builder.featuresToEnable(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_SINGLE_QUOTES);
        };
    }
}
