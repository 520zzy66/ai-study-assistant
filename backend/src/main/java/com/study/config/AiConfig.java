package com.study.config;

import com.study.ai.memory.BoundedChatMemory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 配置类
 *
 * <h3>ChatModel 架构</h3>
 * <ul>
 *   <li><b>openAiChatModel</b> — Spring AI 自动配置，参赛 Docker 默认讯飞星火 Lite，专家 Agent 使用</li>
 *   <li><b>GeneralNode</b> — AgentClientFactory 根据 agent-general.yml 创建云端路由模型</li>
 *   <li><b>Embedding</b> — DashScopeEmbeddingProvider 手动调用百炼接口，并由适配器接入 Spring AI</li>
 * </ul>
 *
 * <p>不需要手动创建 ollamaChatModel bean，避免与 openAiChatModel 冲突。</p>
 */
@Configuration
public class AiConfig {

    /** 对话记忆窗口大小 */
    public static final int CHAT_MEMORY_SIZE = 10;

    // ==================== ChatMemory ====================

    @Bean
    public ChatMemory chatMemory() {
        return new BoundedChatMemory(MessageWindowChatMemory.builder().build());
    }

    // ==================== ChatClient（OpenAI-compatible，专家 Agent） ====================

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("你是一个专业的学习助手，擅长解答学习问题、总结知识点和生成练习题。回复必须使用 Markdown 结构化格式：用 ##/### 标题分层、关键概念加粗、并列内容用列表、对比数据用表格、段落不超过 3 行、块之间空行分隔。禁止大段纯文本堆砌。")
                .build();
    }
}
