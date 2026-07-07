package com.study.ai.client;

import com.study.common.BusinessException;
import com.study.config.AiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.function.Supplier;

/**
 * AI 客户端封装
 * 封装 Spring AI ChatClient，提供统一的 AI 调用接口
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiClient {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    /**
     * 同步调用 AI（无对话记忆）
     * 用于一次性任务，如文档总结、出题等
     *
     * @param prompt 提示词
     * @return AI 回复内容
     */
    public String chat(String prompt) {
        log.info("AI 调用（无记忆），prompt 长度: {}", prompt.length());
        return handleException(() -> {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            log.info("AI 响应成功，响应长度: {}", response != null ? response.length() : 0);
            return response;
        });
    }

    /**
     * 同步调用 AI（带对话记忆）
     * 用于多轮对话场景，如 RAG 问答
     * 基于 userId 隔离不同用户的对话历史
     *
     * @param prompt  提示词
     * @param userId  用户 ID，用于隔离对话历史
     * @return AI 回复内容
     */
    public String chatWithMemory(String prompt, Long userId) {
        log.info("AI 调用（带记忆），userId: {}, prompt 长度: {}", userId, prompt.length());
        String conversationId = "user_" + userId;
        return handleException(() -> {
            String response = chatClient.prompt()
                    .user(prompt)
                    .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                            .conversationId(conversationId)
                            .build())
                    .call()
                    .content();
            log.info("AI 响应成功，响应长度: {}", response != null ? response.length() : 0);
            return response;
        });
    }

    /**
     * 同步调用 AI（带系统提示词，无对话记忆）
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @return AI 回复内容
     */
    public String chat(String systemPrompt, String userPrompt) {
        log.info("AI 调用（无记忆），系统提示词长度: {}, 用户提示词长度: {}",
                systemPrompt.length(), userPrompt.length());
        return handleException(() -> {
            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();
            log.info("AI 响应成功，响应长度: {}", response != null ? response.length() : 0);
            return response;
        });
    }

    /**
     * 同步调用 AI（带系统提示词和对话记忆）
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @param userId       用户 ID，用于隔离对话历史
     * @return AI 回复内容
     */
    public String chatWithMemory(String systemPrompt, String userPrompt, Long userId) {
        log.info("AI 调用（带记忆），userId: {}, 系统提示词长度: {}, 用户提示词长度: {}",
                userId, systemPrompt.length(), userPrompt.length());
        String conversationId = "user_" + userId;
        return handleException(() -> {
            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                            .conversationId(conversationId)
                            .build())
                    .call()
                    .content();
            log.info("AI 响应成功，响应长度: {}", response != null ? response.length() : 0);
            return response;
        });
    }

    /**
     * 流式调用 AI
     * 使用 onBackpressureBuffer + limitRate 防止慢消费者导致的背压问题
     *
     * @param prompt 提示词
     * @return 流式响应
     */
    public Flux<String> chatStream(String prompt) {
        log.info("AI 流式调用，prompt 长度: {}", prompt.length());
        return chatClient.prompt()
                .user(prompt)
                .stream()
                .content()
                .onBackpressureBuffer(256)
                .limitRate(64)
                .onErrorMap(e -> {
                    log.error("AI 流式调用失败: {}", e.getMessage(), e);
                    return new BusinessException(3003, "AI 服务暂时不可用，请稍后重试");
                });
    }

    /**
     * 流式调用 AI（带对话记忆）
     * 用于多轮对话场景，如 RAG 问答的流式输出
     *
     * @param prompt 提示词
     * @param userId 用户 ID，用于隔离对话历史
     * @return 流式响应
     */
    public Flux<String> chatStreamWithMemory(String prompt, Long userId) {
        log.info("AI 流式调用（带记忆），userId: {}, prompt 长度: {}", userId, prompt.length());
        String conversationId = "user_" + userId;
        return chatClient.prompt()
                .user(prompt)
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                        .conversationId(conversationId)
                        .build())
                .stream()
                .content()
                .onBackpressureBuffer(256)
                .limitRate(64)
                .onErrorMap(e -> {
                    log.error("AI 流式调用失败: {}", e.getMessage(), e);
                    return new BusinessException(3003, "AI 服务暂时不可用，请稍后重试");
                });
    }

    /**
     * 获取完整的 ChatResponse（包含元数据）
     *
     * @param prompt 提示词
     * @return ChatResponse
     */
    public ChatResponse chatWithResponse(String prompt) {
        log.info("AI 调用（带响应元数据），prompt 长度: {}", prompt.length());
        return handleException(() -> {
            ChatResponse response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .chatResponse();
            log.info("AI 响应成功，Token 使用: {}",
                    response != null && response.getMetadata() != null
                            ? response.getMetadata().getUsage()
                            : "unknown");
            return response;
        });
    }

    /**
     * 同步调用 AI（指定 temperature）
     * 用于需要不同随机性的场景，如出题需要更低 temperature 保证格式稳定。
     * 当前使用 OpenAiChatOptions（DeepSeek 兼容 OpenAI 协议），
     * 若切换非 OpenAI 协议模型需替换为对应 ChatOptions 实现。
     *
     * @param prompt      提示词
     * @param temperature 温度参数（0.0~2.0），null 使用默认值
     * @return AI 回复内容
     */
    public String chat(String prompt, Double temperature) {
        log.info("AI 调用（temperature={}），prompt 长度: {}", temperature, prompt.length());
        return handleException(() -> {
            var call = chatClient.prompt().user(prompt);
            if (temperature != null) {
                // DeepSeek 兼容 OpenAI 协议，使用 OpenAiChatOptions
                call = call.options(
                        org.springframework.ai.openai.OpenAiChatOptions.builder()
                                .temperature(temperature)
                                .build()
                );
            }
            String response = call.call().content();
            log.info("AI 响应成功，响应长度: {}", response != null ? response.length() : 0);
            return response;
        });
    }

    /**
     * 统一的 AI 调用接口（推荐使用）
     * 通过 ChatRequest.Builder 灵活组合参数
     *
     * @param request 调用请求参数
     * @return AI 回复内容
     */
    public String chat(ChatRequest request) {
        boolean useMemory = request.getUserId() != null;
        boolean hasSystemPrompt = request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank();

        log.info("AI 调用（{}），{}{}prompt 长度: {}",
                useMemory ? "带记忆" : "无记忆",
                hasSystemPrompt ? "系统提示词长度: " + request.getSystemPrompt().length() + ", " : "",
                "用户提示词长度: " + request.getPrompt().length(),
                request.getPrompt().length());

        return handleException(() -> {
            var callBuilder = chatClient.prompt();

            if (hasSystemPrompt) {
                callBuilder = callBuilder.system(request.getSystemPrompt());
            }

            callBuilder = callBuilder.user(request.getPrompt());

            // 设置对话记忆
            if (useMemory) {
                String conversationId = "user_" + request.getUserId();
                callBuilder = callBuilder.advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                        .conversationId(conversationId)
                        .build());
            }

            // 设置 temperature
            if (request.getTemperature() != null) {
                callBuilder = callBuilder.options(
                        org.springframework.ai.openai.OpenAiChatOptions.builder()
                                .temperature(request.getTemperature())
                                .build()
                );
            }

            String response = callBuilder.call().content();
            log.info("AI 响应成功，响应长度: {}", response != null ? response.length() : 0);
            return response;
        });
    }

    /**
     * 统一的 AI 调用异常处理
     * 内部异常详情仅记录日志，对外返回通用消息避免泄露技术细节
     *
     * @param call AI 调用逻辑
     * @param <T>  返回类型
     * @return 调用结果
     */
    private <T> T handleException(Supplier<T> call) {
        try {
            return call.get();
        } catch (BusinessException e) {
            throw e;
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("AI 服务连接超时", e);
            throw new BusinessException(3004, "AI 服务响应超时，请稍后重试");
        } catch (Exception e) {
            log.error("AI 调用失败: {}", e.getMessage(), e);
            throw new BusinessException(3003, "AI 服务暂时不可用，请稍后重试");
        }
    }
}
