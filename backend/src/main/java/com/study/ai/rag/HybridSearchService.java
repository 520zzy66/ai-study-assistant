package com.study.ai.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.entity.MaterialChunk;
import com.study.mapper.MaterialChunkMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 混合检索服务
 * 向量语义检索 + BM25 关键词检索 → RRF 融合排序
 *
 * <p>融合策略：RRF（Reciprocal Rank Fusion）
 * <pre>
 * RRF_score(d) = Σ 1 / (k + rank_i(d))
 * </pre>
 * k = 60 为经验常数，无需归一化即可融合不同量纲的分数。
 *
 * <p>检索流程：
 * <ol>
 *   <li>向量检索 Top-K（默认 10）获取语义相关切片</li>
 *   <li>BM25 检索 Top-K（默认 10）获取关键词匹配切片</li>
 *   <li>RRF 融合两个排序列表</li>
 *   <li>截取 Final Top-K（默认 5）送入 LLM</li>
 * </ol>
 */
@Slf4j
@Service
public class HybridSearchService {

    /** RRF 经验常数（论文推荐值 60） */
    private static final double RRF_K = 60.0;

    /** 单次检索最大加载切片数 */
    private static final int MAX_CHUNKS_PER_SEARCH = 200;

    private final MaterialChunkMapper chunkMapper;
    private final VectorStore vectorStore;
    private final Bm25Service bm25Service;

    /** 向量检索返回候选数 */
    @Value("${ai.rag.vector-top-k:10}")
    private int vectorTopK;

    /** BM25 检索返回候选数 */
    @Value("${ai.rag.bm25-top-k:10}")
    private int bm25TopK;

    /** 最终送入 LLM 的切片数 */
    @Value("${ai.rag.final-top-k:5}")
    private int finalTopK;

    /** 向量相似度阈值 */
    @Value("${ai.rag.similarity-threshold:0.3}")
    private double similarityThreshold;

    /**
     * @param chunkMapper  切片 Mapper（必需）
     * @param vectorStore  向量存储（可选，未配置时仅用 BM25）
     * @param bm25Service  BM25 检索服务（必需）
     */
    public HybridSearchService(MaterialChunkMapper chunkMapper,
                               @org.springframework.beans.factory.annotation.Autowired(required = false) VectorStore vectorStore,
                               Bm25Service bm25Service) {
        this.chunkMapper = chunkMapper;
        this.vectorStore = vectorStore;
        this.bm25Service = bm25Service;
    }

    /**
     * 混合检索（向量 + BM25 → RRF 融合）
     *
     * @param materialId 资料 ID
     * @param userId     用户 ID（安全隔离；materialId 和 userId 至少传一个）
     * @param query      查询文本
     * @param topK       最终返回数（覆盖配置项 ai.rag.final-top-k）
     * @return 融合后的搜索结果列表
     */
    public List<ChunkSearchResult> search(Long materialId, Long userId, String query, int topK) {
        if (topK <= 0) topK = finalTopK;
        if (query == null || query.isBlank()) {
            return List.of();
        }
        if (materialId == null && userId == null) {
            log.warn("Reject hybrid search without materialId or userId.");
            return List.of();
        }

        // 1. 向量语义检索
        List<ChunkSearchResult> vectorResults = searchVector(materialId, userId, query, vectorTopK);

        // 2. BM25 关键词检索
        List<ChunkSearchResult> bm25Results = searchBm25(materialId, userId, query, bm25TopK);

        // 3. RRF 融合
        List<ChunkSearchResult> merged = fuse(vectorResults, bm25Results, topK);

        log.debug("混合检索完成：vector={}, bm25={}, merged={}, materialId={}",
                vectorResults.size(), bm25Results.size(), merged.size(), materialId);

        return merged;
    }

    /**
     * 重载方法：使用配置默认值
     */
    public List<ChunkSearchResult> search(Long materialId, Long userId, String query) {
        return search(materialId, userId, query, finalTopK);
    }

    // ==================== 向量检索 ====================

    private List<ChunkSearchResult> searchVector(Long materialId, Long userId, String query, int topK) {
        if (vectorStore == null) {
            return List.of();
        }

        try {
            var requestBuilder = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(similarityThreshold);

            String filterExpression = buildMetadataFilter(materialId, userId);
            if (!filterExpression.isBlank()) {
                requestBuilder.filterExpression(filterExpression);
            }

            SearchRequest request = requestBuilder.build();

            List<Document> documents = vectorStore.similaritySearch(request);

            if (documents == null || documents.isEmpty()) {
                return List.of();
            }

            return documents.stream()
                    .map(doc -> {
                        Long chunkId = getLongFromMetadata(doc.getMetadata(), "chunk_id");
                        Integer chunkIndex = getIntegerFromMetadata(doc.getMetadata(), "chunk_index");
                        return new ChunkSearchResult(
                                chunkId,
                                chunkIndex != null ? chunkIndex : 0,
                                doc.getText(),
                                0.0  // RRF 只使用排名，分数设为 0
                        );
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("向量检索异常: materialId={}, error={}", materialId, e.getMessage());
            return List.of();
        }
    }

    // ==================== BM25 检索 ====================

    private List<ChunkSearchResult> searchBm25(Long materialId, Long userId, String query, int topK) {
        // 加载切片
        LambdaQueryWrapper<MaterialChunk> wrapper = new LambdaQueryWrapper<MaterialChunk>()
                .orderByAsc(MaterialChunk::getChunkIndex)
                .last("LIMIT " + MAX_CHUNKS_PER_SEARCH);
        if (materialId != null) {
            wrapper.eq(MaterialChunk::getMaterialId, materialId);
        }
        if (userId != null) {
            wrapper.eq(MaterialChunk::getUserId, userId);
        }

        List<MaterialChunk> chunks = chunkMapper.selectList(wrapper);

        if (chunks.isEmpty()) {
            return List.of();
        }

        // 转换为 BM25 输入格式
        List<Bm25Service.ScoredChunk> scoredChunks = chunks.stream()
                .map(c -> new Bm25Service.ScoredChunk(c.getId(), c.getChunkIndex(), c.getContent()))
                .collect(Collectors.toList());

        return bm25Service.search(scoredChunks, query, topK);
    }

    // ==================== RRF 融合 ====================

    /**
     * RRF 融合：合并两个排序列表
     *
     * @param vectorResults 向量检索结果（已按分数降序）
     * @param bm25Results   BM25 检索结果（已按分数降序）
     * @param topK          最终返回数
     * @return 融合后的结果（按 RRF 分数降序）
     */
    private List<ChunkSearchResult> fuse(List<ChunkSearchResult> vectorResults,
                                         List<ChunkSearchResult> bm25Results,
                                         int topK) {
        // 按 chunkId 聚合 RRF 分数
        Map<Long, Double> rrfScores = new HashMap<>();
        Map<Long, ChunkSearchResult> chunkMap = new HashMap<>();

        // 向量检索贡献分数
        for (int i = 0; i < vectorResults.size(); i++) {
            ChunkSearchResult result = vectorResults.get(i);
            double rrfScore = 1.0 / (RRF_K + (i + 1)); // rank 从 1 开始
            rrfScores.merge(result.getChunkId(), rrfScore, Double::sum);
            chunkMap.put(result.getChunkId(), result);
        }

        // BM25 检索贡献分数
        for (int i = 0; i < bm25Results.size(); i++) {
            ChunkSearchResult result = bm25Results.get(i);
            double rrfScore = 1.0 / (RRF_K + (i + 1));
            rrfScores.merge(result.getChunkId(), rrfScore, Double::sum);
            // 如果向量检索未命中此切片，补充到 map
            chunkMap.putIfAbsent(result.getChunkId(), result);
        }

        // 按 RRF 分数降序排列
        List<Map.Entry<Long, Double>> sortedEntries = rrfScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topK)
                .toList();

        // 构建最终结果
        return sortedEntries.stream()
                .map(entry -> {
                    ChunkSearchResult original = chunkMap.get(entry.getKey());
                    // 用 RRF 分数替换原始分数
                    return new ChunkSearchResult(
                            original.getChunkId(),
                            original.getChunkIndex(),
                            original.getContent(),
                            entry.getValue()
                    );
                })
                .collect(Collectors.toList());
    }

    // ==================== 元数据解析工具 ====================

    private Long getLongFromMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private Integer getIntegerFromMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private String buildMetadataFilter(Long materialId, Long userId) {
        List<String> filters = new ArrayList<>();
        if (materialId != null) {
            filters.add("material_id == " + materialId);
        }
        if (userId != null) {
            filters.add("user_id == " + userId);
        }
        return String.join(" AND ", filters);
    }
}
