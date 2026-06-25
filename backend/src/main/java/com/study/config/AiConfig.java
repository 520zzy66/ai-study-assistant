package com.study.config;

import com.study.ai.memory.BoundedChatMemory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 配置类
 * 配置 Spring AI ChatClient
 */
@Configuration
public class AiConfig {

    /**
     * 对话记忆窗口大小，限制每次请求携带的历史消息轮数，防止内存无限增长
     */
    public static final int CHAT_MEMORY_SIZE = 10;

    /**
     * ChatMemory Bean，用于存储对话历史
     * 使用 BoundedChatMemory 包装 InMemoryChatMemory，限制最大 1000 个会话，LRU 淘汰旧会话防止 OOM
     */
    @Bean
    public ChatMemory chatMemory() {
        return new BoundedChatMemory(new InMemoryChatMemory());
    }

    /**
     * 创建 ChatClient Bean，用于调用 DeepSeek API（兼容 OpenAI 协议）
     * 注意：不在此处设置默认的 MessageChatMemoryAdvisor，
     * 因为需要在调用时动态传入 conversation_id 实现多用户隔离
     *
     * @param builder ChatClient.Builder
     * @return ChatClient
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("你是一个专业的学习助手，擅长解答学习问题、总结知识点和生成练习题。回复必须使用 Markdown 结构化格式：用 ##/### 标题分层、关键概念加粗、并列内容用列表、对比数据用表格、段落不超过 3 行、块之间空行分隔。禁止大段纯文本堆砌。")
                .build();
    }
}
