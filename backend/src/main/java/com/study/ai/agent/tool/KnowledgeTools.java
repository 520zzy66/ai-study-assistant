package com.study.ai.agent.tool;

import com.study.ai.rag.ChunkSearchResult;
import com.study.ai.rag.HybridSearchService;
import com.study.common.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring AI native tools exposed to expert agents.
 *
 * <p>Tool methods always return JSON arrays. On invalid input or backend
 * failures they return {@code []} so a ReAct loop can continue gracefully.</p>
 */
@Slf4j
@Component
public class KnowledgeTools {

    private static final int DEFAULT_TOP_K = 5;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.3;
    private static final String EMPTY_JSON_ARRAY = "[]";

    private final HybridSearchService hybridSearchService;
    private final VectorStore vectorStore;

    public KnowledgeTools(HybridSearchService hybridSearchService,
                          @Autowired(required = false) VectorStore vectorStore) {
        this.hybridSearchService = hybridSearchService;
        this.vectorStore = vectorStore;
    }

    /**
     * Search the built-in system knowledge bank for a domain-specific answer.
     *
     * @param domain     exam domain, such as CIVIL, GRADUATE or GENERAL
     * @param query      search keywords or the user question
     * @param folderName optional specific folder name to restrict search range (e.g. '素材金句积累', '判断推理', '蒙题秒杀技巧', '党政党史', '政策热点')
     * @return JSON array of matched knowledge chunks
     */
    @Tool(description = "Search the built-in exam knowledge bank by domain and optional folder name for precise scoping")
    public String searchDomainKnowledge(
            @ToolParam(description = "Exam domain: CIVIL, GRADUATE or GENERAL") String domain,
            @ToolParam(description = "Search keywords or user question") String query,
            @ToolParam(description = "Optional folder name to narrow down search range, e.g. '素材金句积累', '判断推理', '蒙题秒杀技巧', '党政党史', '政策热点'") String folderName) {
        long start = System.currentTimeMillis();
        String normalizedDomain = normalizeDomain(domain);
        ToolCallEventPublisher.toolCall("searchDomainKnowledge", params(
                "domain", normalizedDomain,
                "query", truncate(query),
                "folderName", truncate(folderName)));
        if (isBlank(query) || vectorStore == null) {
            ToolCallEventPublisher.toolResult("searchDomainKnowledge", 0, elapsed(start), false);
            return EMPTY_JSON_ARRAY;
        }

        String filterExpression = "source == 'system_knowledge_bank' AND domain == '" + normalizedDomain + "'";
        if (!isBlank(folderName)) {
            filterExpression += " AND folderName == '" + folderName.trim().replace("'", "\\'") + "'";
        }
        try {
            List<Document> documents = searchVectorStore(query, filterExpression, DEFAULT_TOP_K);
            ToolCallEventPublisher.toolResult("searchDomainKnowledge", documents.size(), elapsed(start), false);
            return toJsonArray(documents.stream()
                    .map(doc -> toDocumentPayload(doc, "system_knowledge"))
                    .toList());
        } catch (Exception e) {
            log.warn("Domain knowledge search failed: domain={}, error={}", normalizedDomain, e.getMessage());
            ToolCallEventPublisher.toolResult("searchDomainKnowledge", 0, elapsed(start), true);
            return EMPTY_JSON_ARRAY;
        }
    }

    /**
     * Search the current user's uploaded study materials.
     *
     * @param userId   current user ID
     * @param query    search keywords or the user question
     * @param quizType optional quiz or exam type hint
     * @return JSON array of matched user material chunks
     */
    @Tool(description = "Search the current user's uploaded study materials")
    public String searchPersonalMaterial(
            @ToolParam(description = "Current user ID") Long userId,
            @ToolParam(description = "Search keywords or user question") String query,
            @ToolParam(description = "Optional quiz or exam type hint") String quizType) {
        long start = System.currentTimeMillis();
        ToolCallEventPublisher.toolCall("searchPersonalMaterial", params(
                "userId", userId,
                "query", truncate(query),
                "quizType", truncate(quizType)));
        if (userId == null || userId <= 0 || isBlank(query)) {
            ToolCallEventPublisher.toolResult("searchPersonalMaterial", 0, elapsed(start), false);
            return EMPTY_JSON_ARRAY;
        }

        String enhancedQuery = isBlank(quizType) ? query : quizType + " " + query;
        try {
            List<ChunkSearchResult> results = hybridSearchService.search(null, userId, enhancedQuery, DEFAULT_TOP_K);
            ToolCallEventPublisher.toolResult("searchPersonalMaterial", results.size(), elapsed(start), false);
            return toJsonArray(results.stream()
                    .map(this::toChunkPayload)
                    .toList());
        } catch (Exception e) {
            log.warn("Personal material search failed: userId={}, error={}", userId, e.getMessage());
            ToolCallEventPublisher.toolResult("searchPersonalMaterial", 0, elapsed(start), true);
            return EMPTY_JSON_ARRAY;
        }
    }

    /**
     * Search semantic chunks from the current user's conversation history.
     *
     * @param userId current user ID
     * @param query  search keywords or the user question
     * @return JSON array of matched conversation history chunks
     */
    @Tool(description = "Search the current user's conversation history")
    public String searchConversationHistory(
            @ToolParam(description = "Current user ID") Long userId,
            @ToolParam(description = "Search keywords or user question") String query) {
        long start = System.currentTimeMillis();
        ToolCallEventPublisher.toolCall("searchConversationHistory", params(
                "userId", userId,
                "query", truncate(query)));
        if (userId == null || userId <= 0 || isBlank(query) || vectorStore == null) {
            ToolCallEventPublisher.toolResult("searchConversationHistory", 0, elapsed(start), false);
            return EMPTY_JSON_ARRAY;
        }

        String filterExpression = "source == 'conversation_history' AND user_id == " + userId;
        try {
            List<Document> documents = searchVectorStore(query, filterExpression, DEFAULT_TOP_K);
            ToolCallEventPublisher.toolResult("searchConversationHistory", documents.size(), elapsed(start), false);
            return toJsonArray(documents.stream()
                    .map(doc -> toDocumentPayload(doc, "conversation_history"))
                    .toList());
        } catch (Exception e) {
            log.warn("Conversation history search failed: userId={}, error={}", userId, e.getMessage());
            ToolCallEventPublisher.toolResult("searchConversationHistory", 0, elapsed(start), true);
            return EMPTY_JSON_ARRAY;
        }
    }

    private List<Document> searchVectorStore(String query, String filterExpression, int topK) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(DEFAULT_SIMILARITY_THRESHOLD)
                .filterExpression(filterExpression)
                .build();
        List<Document> documents = vectorStore.similaritySearch(request);
        return documents == null ? List.of() : documents;
    }

    private Map<String, Object> toDocumentPayload(Document document, String source) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("source", source);
        payload.put("documentId", document.getId());
        payload.put("content", document.getText());
        payload.put("metadata", document.getMetadata());
        return payload;
    }

    private Map<String, Object> toChunkPayload(ChunkSearchResult result) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("source", "user_material");
        payload.put("chunkId", result.getChunkId());
        payload.put("chunkIndex", result.getChunkIndex());
        payload.put("content", result.getContent());
        payload.put("score", result.getScore());
        return payload;
    }

    private String toJsonArray(Object value) {
        return JsonUtils.toJson(value, EMPTY_JSON_ARRAY);
    }

    private String normalizeDomain(String domain) {
        if (isBlank(domain)) {
            return "GENERAL";
        }
        String normalized = domain.trim().toUpperCase();
        return switch (normalized) {
            case "CIVIL", "GRADUATE", "GENERAL" -> normalized;
            default -> "GENERAL";
        };
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 120 ? value : value.substring(0, 120) + "...";
    }

    private Map<String, Object> params(Object... pairs) {
        Map<String, Object> params = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            params.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return params;
    }
}
