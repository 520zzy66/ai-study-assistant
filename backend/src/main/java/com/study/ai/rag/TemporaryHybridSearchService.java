package com.study.ai.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.entity.TemporaryMaterialChunk;
import com.study.mapper.TemporaryMaterialChunkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 临时资料混合检索（向量 + BM25 + RRF）。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TemporaryHybridSearchService {

    private static final double RRF_K = 60.0;
    private final TemporaryMaterialChunkMapper chunkMapper;
    private final VectorStore vectorStore;
    private final Bm25Service bm25Service;

    /**
     * 在可信的用户、会话和上传令牌范围内检索。
     */
    public List<ChunkSearchResult> search(Long userId, String conversationId,
                                          String uploadToken, String query, int topK) {
        if (userId == null || isBlank(conversationId) || isBlank(uploadToken) || isBlank(query)) {
            return List.of();
        }
        List<ChunkSearchResult> vector;
        try {
            vector = vectorSearch(userId, conversationId, uploadToken, query, 10);
        } catch (Exception e) {
            log.warn("临时资料向量检索失败，降级为BM25: token={}, error={}", uploadToken, e.getMessage());
            vector = List.of();
        }
        List<TemporaryMaterialChunk> chunks = chunkMapper.selectList(
                new LambdaQueryWrapper<TemporaryMaterialChunk>()
                        .eq(TemporaryMaterialChunk::getUserId, userId)
                        .eq(TemporaryMaterialChunk::getConversationId, conversationId)
                        .eq(TemporaryMaterialChunk::getUploadToken, uploadToken)
                        .orderByAsc(TemporaryMaterialChunk::getChunkIndex));
        List<Bm25Service.ScoredChunk> candidates = chunks.stream()
                .map(c -> new Bm25Service.ScoredChunk(c.getId(), c.getChunkIndex(), c.getContent()))
                .toList();
        List<ChunkSearchResult> bm25 = bm25Service.search(candidates, query, 10);
        return fuse(vector, bm25, topK > 0 ? topK : 5);
    }

    private List<ChunkSearchResult> vectorSearch(Long userId, String conversationId,
                                                  String uploadToken, String query, int topK) {
        String escapedConversation = conversationId.replace("'", "\\'");
        String escapedToken = uploadToken.replace("'", "\\'");
        String filter = "source == 'temporary_material' AND user_id == " + userId
                + " AND conversation_id == '" + escapedConversation + "'"
                + " AND upload_token == '" + escapedToken + "'";
        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(0.3)
                .filterExpression(filter)
                .build());
        if (documents == null) {
            return List.of();
        }
        List<ChunkSearchResult> results = new ArrayList<>(documents.size());
        for (Document document : documents) {
            Object rawId = document.getMetadata().get("chunk_id");
            Object rawIndex = document.getMetadata().get("chunk_index");
            Long id = rawId instanceof Number n ? n.longValue() : Long.valueOf(String.valueOf(rawId));
            int index = rawIndex instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(rawIndex));
            results.add(new ChunkSearchResult(id, index, document.getText(), 0.0));
        }
        return results;
    }

    private List<ChunkSearchResult> fuse(List<ChunkSearchResult> vector,
                                          List<ChunkSearchResult> bm25, int topK) {
        Map<Long, Double> scores = new HashMap<>();
        Map<Long, ChunkSearchResult> values = new HashMap<>();
        addRanks(vector, scores, values);
        addRanks(bm25, scores, values);
        return scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(topK)
                .map(entry -> {
                    ChunkSearchResult value = values.get(entry.getKey());
                    return new ChunkSearchResult(value.getChunkId(), value.getChunkIndex(),
                            value.getContent(), entry.getValue());
                })
                .toList();
    }

    private void addRanks(List<ChunkSearchResult> ranked, Map<Long, Double> scores,
                          Map<Long, ChunkSearchResult> values) {
        for (int i = 0; i < ranked.size(); i++) {
            ChunkSearchResult value = ranked.get(i);
            scores.merge(value.getChunkId(), 1.0 / (RRF_K + i + 1), Double::sum);
            values.putIfAbsent(value.getChunkId(), value);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
