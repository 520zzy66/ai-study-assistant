package com.study.ai.service;

import com.study.ai.client.AiClient;
import com.study.ai.prompt.PromptTemplates;
import com.study.ai.rag.ChunkSearchResult;
import com.study.ai.rag.RagService;
import com.study.ai.validator.MaterialValidator;
import com.study.common.BusinessException;
import com.study.common.Constants;
import com.study.common.UserContext;
import com.study.entity.AiChatHistory;
import com.study.entity.LearningMaterial;
import com.study.mapper.AiChatHistoryMapper;
import com.study.mapper.LearningMaterialMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AI 文档问答服务（RAG）
 * 基于检索增强生成（RAG）回答用户问题
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiQaService {

    private final AiClient aiClient;
    private final RagService ragService;
    private final MaterialValidator materialValidator;
    private final AiChatHistoryMapper chatHistoryMapper;

    /**
     * 同步问答
     *
     * @param materialId 资料ID
     * @param question   用户问题
     * @return 包含 answer、sources、conversationId 的 Map
     */
    public Map<String, Object> ask(Long materialId, String question) {
        Long userId = UserContext.getCurrentUserId();

        // 1. 校验资料
        materialValidator.validateAndGet(materialId, userId);

        // 2. 检索相关切片
        List<ChunkSearchResult> sources = ragService.search(materialId, question, 5);

        // 3. 无相关切片时明确告知
        if (sources.isEmpty()) {
            String answer = "资料中未找到与您问题相关的内容，请尝试换个问法或检查资料内容。";
            String conversationId = saveHistory(userId, materialId, question, answer, null);
            return Map.of("answer", answer, "sources", List.of(), "conversationId", conversationId);
        }

        // 4. 拼接上下文
        String context = buildContext(sources);

        // 5. 构建 Prompt 并调用 AI
        String prompt = PromptTemplates.buildQaPrompt(context, question);
        String answer = aiClient.chat(prompt);

        // 6. 保存历史
        String conversationId = saveHistory(userId, materialId, question, answer, null);

        // 7. 构建返回
        List<Map<String, Object>> sourceList = sources.stream()
                .map(s -> Map.<String, Object>of(
                        "chunkIndex", s.getChunkIndex(),
                        "content", s.getContent(),
                        "score", s.getScore()
                ))
                .collect(Collectors.toList());

        return Map.of(
                "answer", answer,
                "sources", sourceList,
                "conversationId", conversationId
        );
    }

    /**
     * 流式问答（SSE）
     *
     * @param materialId 资料ID
     * @param question   用户问题
     * @return 流式响应 Flux
     */
    public Flux<String> askStream(Long materialId, String question) {
        Long userId = UserContext.getCurrentUserId();

        // 1. 校验资料
        materialValidator.validateAndGet(materialId, userId);

        // 2. 检索相关切片
        List<ChunkSearchResult> sources = ragService.search(materialId, question, 5);

        // 3. 无相关切片时提前返回，与同步 ask() 保持一致
        if (sources.isEmpty()) {
            String answer = "资料中未找到与您问题相关的内容，请尝试换个问法或检查资料内容。";
            String conversationId = saveHistory(userId, materialId, question, answer, null);
            return Flux.just(answer, "\n\n[conversationId: " + conversationId + "]");
        }

        // 4. 拼接上下文
        String context = buildContext(sources);

        // 5. 构建 Prompt
        String prompt = PromptTemplates.buildQaPrompt(context, question);

        // 6. 流式调用 AI，完成后保存历史
        StringBuilder fullResponse = new StringBuilder();
        return aiClient.chatStream(prompt)
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> saveHistory(userId, materialId, question,
                        fullResponse.toString(), null))
                .doOnError(e -> log.error("RAG 流式问答失败: materialId={}, question={}",
                        materialId, question, e))
                .onErrorResume(e -> Flux.just("\n\n[错误: 回答生成失败，请稍后重试]"));
    }

    /**
     * 拼接检索到的切片为上下文
     */
    private String buildContext(List<ChunkSearchResult> sources) {
        if (sources.isEmpty()) {
            return "（无参考资料）";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sources.size(); i++) {
            ChunkSearchResult s = sources.get(i);
            sb.append("[片段").append(i + 1).append("] (相似度: ")
                    .append(String.format("%.2f", s.getScore()))
                    .append(")\n")
                    .append(s.getContent())
                    .append("\n\n");
        }
        return sb.toString();
    }

    /**
     * 保存对话历史
     */
    private String saveHistory(Long userId, Long materialId, String question,
                               String answer, String conversationId) {
        if (conversationId == null) {
            conversationId = UUID.randomUUID().toString().substring(0, 8);
        }
        AiChatHistory history = new AiChatHistory();
        history.setUserId(userId);
        history.setChatType(Constants.CHAT_TYPE_QA);
        history.setMaterialId(materialId);
        history.setUserMessage(question);
        history.setAiResponse(answer);
        history.setConversationId(conversationId);
        chatHistoryMapper.insert(history);
        return conversationId;
    }
}
