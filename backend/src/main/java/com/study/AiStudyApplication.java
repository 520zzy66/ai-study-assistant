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
 * 排除 OllamaChatAutoConfiguration：Ollama 仅用于 Embedding（bge-m3），
 * ChatModel 只需 openAiChatModel（小米 MiMo），避免 bean 冲突。
 * GeneralNode 的 Ollama 模型由 AgentClientFactory 根据 agent-general.yml 独立创建。
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
