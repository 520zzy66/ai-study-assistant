package com.study.ai.service;

import com.study.ai.MaterialContentReader;
import com.study.ai.client.AiClient;
import com.study.ai.prompt.PromptTemplates;
import com.study.ai.rag.ChunkSearchResult;
import com.study.ai.rag.RagService;
import com.study.ai.validator.MaterialValidator;
import com.study.common.Constants;
import com.study.common.UserContext;
import com.study.dto.request.QaRequest;
import com.study.entity.AiChatHistory;
import com.study.entity.LearningMaterial;
import com.study.mapper.AiChatHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private final MaterialContentReader materialContentReader;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    /**
     * 同步问答（支持可选资料）
     */
    public Map<String, Object> ask(Long materialId, String question, List<QaRequest.ChatMessage> history) {
        if (materialId == null) {
            return askGeneral(question, history);
        }

        Long userId = UserContext.getCurrentUserId();
        LearningMaterial material = materialValidator.validateOwnership(materialId, userId);

        // 尝试从 RAG 检索
        List<ChunkSearchResult> sources = ragService.search(materialId, question, 5);

        String context;
        if (!sources.isEmpty()) {
            context = buildContext(sources);
            log.debug("RAG 检索命中 {} 个切片: materialId={}, question={}", sources.size(), materialId, question);
        } else {
            // RAG 无结果，尝试从已处理的切片中读取全部内容
            context = materialContentReader.readContent(materialId);
            if (context != null && !context.isBlank()) {
                log.info("RAG 检索无结果，回退到全量切片内容: materialId={}, len={}", materialId, context.length());
            } else {
                // 切片也没有内容，尝试读取原始文件（仅 txt/md 有效）
                String rawContent = readRawFileContent(material);
                if (rawContent != null && !rawContent.isBlank()) {
                    context = rawContent;
                    log.info("切片为空，回退到原始文件内容: materialId={}", materialId);
                } else {
                    String answer = "资料中未找到与您问题相关的内容，请尝试换个问法或检查资料内容。";
                    String conversationId = saveHistory(userId, materialId, question, answer, null);
                    return Map.of("answer", answer, "sources", List.of(), "conversationId", conversationId);
                }
            }
        }

        String prompt = PromptTemplates.buildQaPromptWithHistory(context, question, history);
        String answer = aiClient.chat(prompt);

        String conversationId = saveHistory(userId, materialId, question, answer, null);

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
     * 通用对话（无需学习资料）
     */
    public Map<String, Object> askGeneral(String question, List<QaRequest.ChatMessage> history) {
        Long userId = UserContext.getCurrentUserId();

        String prompt = PromptTemplates.buildGeneralChatPromptWithHistory(question, history);
        String answer = aiClient.chat(prompt);

        String conversationId = saveHistory(userId, null, question, answer, null);

        return Map.of(
                "answer", answer,
                "sources", List.of(),
                "conversationId", conversationId
        );
    }

    /**
     * 流式问答（SSE，支持可选资料）
     */
    public Flux<String> askStream(Long materialId, String question, List<QaRequest.ChatMessage> history) {
        if (materialId == null) {
            return askStreamGeneral(question, history);
        }

        Long userId = UserContext.getCurrentUserId();
        LearningMaterial material = materialValidator.validateOwnership(materialId, userId);

        // 尝试从 RAG 检索
        List<ChunkSearchResult> sources = ragService.search(materialId, question, 5);

        String context;
        if (!sources.isEmpty()) {
            context = buildContext(sources);
            log.debug("RAG 流式检索命中 {} 个切片: materialId={}, question={}", sources.size(), materialId, question);
        } else {
            // RAG 无结果，尝试从已处理的切片中读取全部内容
            context = materialContentReader.readContent(materialId);
            if (context != null && !context.isBlank()) {
                log.info("RAG 流式检索无结果，回退到全量切片内容: materialId={}, len={}", materialId, context.length());
            } else {
                // 切片也没有内容，尝试读取原始文件（仅 txt/md 有效）
                String rawContent = readRawFileContent(material);
                if (rawContent != null && !rawContent.isBlank()) {
                    context = rawContent;
                    log.info("切片为空，回退到原始文件内容: materialId={}", materialId);
                } else {
                    String answer = "资料中未找到与您问题相关的内容，请尝试换个问法或检查资料内容。";
                    String conversationId = saveHistory(userId, materialId, question, answer, null);
                    return Flux.just(answer, "\n\n[conversationId: " + conversationId + "]");
                }
            }
        }

        String prompt = PromptTemplates.buildQaPromptWithHistory(context, question, history);

        StringBuilder fullResponse = new StringBuilder();
        String convId = UUID.randomUUID().toString().substring(0, 8);
        return aiClient.chatStream(prompt)
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> saveHistory(userId, materialId, question,
                        fullResponse.toString(), convId))
                .doOnError(e -> log.error("RAG 流式问答失败: materialId={}, question={}",
                        materialId, question, e))
                .onErrorResume(e -> Flux.just("\n\n[错误: 回答生成失败，请稍后重试]"))
                .concatWith(Flux.just("\n\n[conversationId: " + convId + "]"));
    }

    /**
     * 通用流式对话（无需学习资料）
     */
    private Flux<String> askStreamGeneral(String question, List<QaRequest.ChatMessage> history) {
        Long userId = UserContext.getCurrentUserId();

        String prompt = PromptTemplates.buildGeneralChatPromptWithHistory(question, history);
        StringBuilder fullResponse = new StringBuilder();
        String convId = UUID.randomUUID().toString().substring(0, 8);

        return aiClient.chatStream(prompt)
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> saveHistory(userId, null, question,
                        fullResponse.toString(), convId))
                .doOnError(e -> log.error("通用流式对话失败: question={}", question, e))
                .onErrorResume(e -> Flux.just("\n\n[错误: 回答生成失败，请稍后重试]"))
                .concatWith(Flux.just("\n\n[conversationId: " + convId + "]"));
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
     * 读取原始文件内容（用于 processing 状态的资料）
     * 使用 InputStream 限制读取量，避免大文件 OOM
     */
    private String readRawFileContent(LearningMaterial material) {
        try {
            Path filePath = Paths.get(uploadDir, material.getFilePath());
            if (!Files.exists(filePath)) {
                return null;
            }
            // 限制最多读取 16000 字节，避免大文件 OOM
            byte[] buf = new byte[16000];
            try (InputStream is = Files.newInputStream(filePath)) {
                int read = is.read(buf);
                if (read <= 0) return null;
                String content = new String(buf, 0, read, StandardCharsets.UTF_8);
                // 检查是否还有更多内容
                if (is.read() != -1) {
                    content += "\n\n[注：文档内容过长，已截断...]";
                }
                return content;
            }
        } catch (Exception e) {
            log.error("读取原始文件失败: materialId={}", material.getId(), e);
            return null;
        }
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
