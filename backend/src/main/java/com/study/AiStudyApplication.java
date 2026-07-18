package com.study;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI 智能学习助手应用入口
 *
 * 排除 OllamaChatAutoConfiguration：Ollama 仅作为可选本地工具模型，
 * 默认 ChatModel 走 OpenAI-compatible provider（参赛 Docker 默认讯飞星火 Lite），避免 bean 冲突。
 * GeneralNode 默认复用云端模型；agent-qwen-tools.yml 保留给本地开发可选使用。
 */
@SpringBootApplication(exclude = {
    RedisAutoConfiguration.class,
    RedisRepositoriesAutoConfiguration.class,
    org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration.class
})
@EnableAsync
@EnableScheduling
public class AiStudyApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiStudyApplication.class, args);
    }
}
