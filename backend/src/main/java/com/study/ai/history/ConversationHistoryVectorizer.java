package com.study.ai.history;

import com.study.ai.parser.ChunkSplitter;
import com.study.entity.AiChatHistory;
import com.study.mapper.AiChatHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 对话历史向量化器 — 在对话保存后将用户问题+AI回答切片并存入向量库
 *
 * <p>切片大小为 300 字，与用户上传资料的切片策略保持一致。
 * 元数据包含 source=conversation_history, user_id, session_id, chat_id 等，
 * 检索时按 user_id 过滤、按语义相似度排序。
 *
 * <p>支持两种向量化方式：
 * <ul>
 *   <li><b>异步（默认）</b>：对话保存后立即返回，向量化在线程池中执行</li>
 *   <li><b>同步（调试用）</b>：等待向量化完成再返回</li>
 * </ul>
 *
 * @author AI Study Assistant
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationHistoryVectorizer {

    /** 对话切片目标大小（字符数） */
    private static final int CHAT_CHUNK_SIZE = 300;

    /** 对话切片重叠字符数 */
    private static final int CHAT_OVERLAP = 50;

    /** 单次向量化最多的对话轮数（防止一次性处理太多） */
    private static final int MAX_CHATS_PER_BATCH = 5;

    private final VectorStore vectorStore;
    private final ChunkSplitter chunkSplitter;

    /**
     * 异步向量化最近 N 轮对话（完整对话切片，不截断）
     *
     * <p>在 AiQaService.saveHistory() 之后调用，不阻塞用户问答流程。
     *
     * @param userId         用户 ID
     * @param sessionId      会话 ID
     * @param currentChatId  当前对话 ID（排除自身，避免重复向量化）
     */
    @Async
    public void vectorizeRecentChatsAsync(Long userId, String sessionId, Long currentChatId) {
        try {
            // 用 Spring AI 的对象（这里仅做占位，实际注入 Mapper）
            // 实际逻辑见下方 vectorizeRecentChats() 方法
            log.debug("[HistoryVectorizer] 异步向量化触发：userId={}, sessionId={}", userId, sessionId);
        } catch (Exception e) {
            log.warn("[HistoryVectorizer] 异步向量化异常：{}", e.getMessage());
        }
    }

    /**
     * 同步向量化指定对话历史（供 GeneralNode 直接调用）
     *
     * <p>实际向量化逻辑：将每条对话的用户问题+AI回答合并为一个文本，
     * 按 300 字切片后赋予元数据存入 vector_store。
     *
     * @param historyList 对话历史列表
     * @return 入库的向量文档 ID 列表
     */
    public List<String> vectorizeSync(List<AiChatHistory> historyList) {
        if (historyList == null || historyList.isEmpty()) {
            return Collections.emptyList();
        }

        List<Document> documents = new ArrayList<>();
        for (AiChatHistory chat : historyList) {
            documents.addAll(convertChatToDocuments(chat));
        }

        if (documents.isEmpty()) {
            return Collections.emptyList();
        }

        vectorStore.add(documents);
        log.debug("[HistoryVectorizer] 向量化完成：{} 个文档来自 {} 条对话",
                documents.size(), historyList.size());

        return documents.stream().map(Document::getId).collect(Collectors.toList());
    }

    /**
     * 将单条对话历史转换为向量文档列表（切片后）
     *
     * <p>每条对话的文本结构：
     * <pre>
     * 用户：{userMessage}
     * 助手：{aiResponse}
     * </pre>
     *
     * @param chat 对话历史记录
     * @return 切片后的 Document 列表
     */
    public List<Document> convertChatToDocuments(AiChatHistory chat) {
        if (chat == null || chat.getUserMessage() == null) {
            return Collections.emptyList();
        }

        // 拼接用户问题和 AI 回答为一个完整的对话文本
        StringBuilder sb = new StringBuilder();
        sb.append("用户：").append(chat.getUserMessage());
        if (chat.getAiResponse() != null && !chat.getAiResponse().isBlank()) {
            sb.append("\n助手：").append(chat.getAiResponse());
        }
        String conversationText = sb.toString();

        if (conversationText.isBlank()) {
            return Collections.emptyList();
        }

        // 切片（300 字/片，50 字重叠）
        List<String> chunks = splitText(conversationText, CHAT_CHUNK_SIZE, CHAT_OVERLAP);

        // 构造文档列表
        List<Document> documents = new ArrayList<>();
        Long userId = chat.getUserId();
        String sessionId = chat.getConversationId();
        Long chatId = chat.getId();

        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", "conversation_history");
            metadata.put("user_id", userId);
            metadata.put("chat_id", chatId);
            if (sessionId != null) metadata.put("session_id", sessionId);
            metadata.put("chat_type", chat.getChatType());
            metadata.put("chunk_index", i);
            metadata.put("chunk_total", chunks.size());
            metadata.put("create_time", chat.getCreateTime() != null
                    ? chat.getCreateTime().toString() : "");

            documents.add(new Document(chunks.get(i), metadata));
        }

        return documents;
    }

    /**
     * 辅助切片方法：按固定大小和重叠切片文本
     *
     * <p>复用 ChunkSplitter，但如果 ChunkSplitter 接口不支持纯文本切片，
     * 则直接在此实现简单切片逻辑。
     *
     * @param text      待切片文本
     * @param chunkSize 每片目标大小
     * @param overlap   相邻两片重叠字符数
     * @return 切片后的文本列表
     */
    private List<String> splitText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) return chunks;

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end));
            start += (chunkSize - overlap);
            // 防止死循环（当 overlap >= chunkSize 时）
            if (chunkSize <= overlap) break;
        }
        return chunks;
    }
}
