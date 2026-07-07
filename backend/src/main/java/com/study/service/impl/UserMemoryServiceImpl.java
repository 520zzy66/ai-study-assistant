package com.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.entity.UserMemoryEntry;
import com.study.mapper.UserMemoryEntryMapper;
import com.study.service.UserMemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户记忆服务实现
 *
 * <p>管理记忆条目的持久化、向量检索和清理。
 * 记忆条目写入 PostgreSQL 后同步写入 PgVectorStore，
 * 检索时使用向量语义召回 + DB 回查完整字段。
 */
@Slf4j
@Service
public class UserMemoryServiceImpl implements UserMemoryService {

    private final UserMemoryEntryMapper memoryEntryMapper;

    /** 向量存储（可选：未配置时仅使用 DB 分层检索） */
    private final VectorStore vectorStore;

    private final JdbcTemplate jdbcTemplate;

    public UserMemoryServiceImpl(UserMemoryEntryMapper memoryEntryMapper,
                                  @org.springframework.beans.factory.annotation.Autowired(required = false) VectorStore vectorStore,
                                  JdbcTemplate jdbcTemplate) {
        this.memoryEntryMapper = memoryEntryMapper;
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 温区边界（天）：超过此天数仅保留高重要度记忆 */
    private static final int WARM_ZONE_DAYS = 90;

    /** 冷区清理阈值：温区以下且 importance 低于此值则清理 */
    private static final float COLD_ZONE_MIN_IMPORTANCE = 0.9f;

    @Override
    public void addMemory(Long userId, String category, String content, float importance,
                          String sourceType, Long sourceId) {
        UserMemoryEntry entry = new UserMemoryEntry();
        entry.setUserId(userId);
        entry.setCategory(category);
        entry.setContent(content);
        entry.setImportance(importance);
        entry.setSourceType(sourceType);
        entry.setSourceId(sourceId);
        memoryEntryMapper.insert(entry);

        // 同步写入向量库
        indexToVectorStore(List.of(entry));
    }

    @Override
    public void addMemories(List<UserMemoryEntry> entries) {
        for (UserMemoryEntry entry : entries) {
            memoryEntryMapper.insert(entry);
        }

        // 同步写入向量库
        indexToVectorStore(entries);
    }

    @Override
    public List<UserMemoryEntry> searchMemories(Long userId, String query, int limit) {
        // 优先使用向量语义检索
        if (vectorStore != null) {
            try {
                List<Long> memoryIds = vectorSearch(userId, query, limit);
                if (!memoryIds.isEmpty()) {
                    // 回查 DB 获取完整字段，保持原排序
                    return fetchEntriesByIds(memoryIds);
                }
            } catch (Exception e) {
                log.warn("向量检索失败，降级到分层检索: userId={}, error={}", userId, e.getMessage());
            }
        }

        // 降级：分层检索（90 天内全量 + 90 天外仅 importance >= 0.7）
        LocalDateTime warmThreshold = LocalDateTime.now().minusDays(WARM_ZONE_DAYS);
        return memoryEntryMapper.selectList(
                new LambdaQueryWrapper<UserMemoryEntry>()
                        .eq(UserMemoryEntry::getUserId, userId)
                        .and(w -> w.gt(UserMemoryEntry::getCreatedAt, warmThreshold)
                                .or()
                                .ge(UserMemoryEntry::getImportance, 0.7f))
                        .orderByDesc(UserMemoryEntry::getImportance)
                        .last("LIMIT " + Math.min(limit, 50))
        );
    }

    @Override
    public List<UserMemoryEntry> listMemories(Long userId, int limit) {
        return memoryEntryMapper.selectList(
                new LambdaQueryWrapper<UserMemoryEntry>()
                        .eq(UserMemoryEntry::getUserId, userId)
                        .orderByDesc(UserMemoryEntry::getImportance)
                        .last("LIMIT " + Math.min(limit, 100))
        );
    }

    @Override
    public int cleanupExpiredMemories() {
        LocalDateTime warmThreshold = LocalDateTime.now().minusDays(WARM_ZONE_DAYS);

        // R8 修复：先删向量（用子查询原子执行，消除 SELECT+DELETE 之间的 TOCTOU 窗口）
        if (vectorStore != null) {
            try {
                int vectorDeleted = jdbcTemplate.update(
                        "DELETE FROM vector_store WHERE (metadata->>'memory_id')::bigint IN " +
                        "(SELECT id FROM user_memory_entry WHERE created_at < ? AND importance < ?)",
                        warmThreshold, COLD_ZONE_MIN_IMPORTANCE);
                if (vectorDeleted > 0) {
                    log.debug("预清理过期记忆向量: {} 条", vectorDeleted);
                }
            } catch (Exception e) {
                log.warn("向量清理失败（不影响 DB 清理）: error={}", e.getMessage());
            }
        }

        // 再删除 DB 记录
        int deleted = memoryEntryMapper.delete(
                new LambdaQueryWrapper<UserMemoryEntry>()
                        .lt(UserMemoryEntry::getCreatedAt, warmThreshold)
                        .lt(UserMemoryEntry::getImportance, COLD_ZONE_MIN_IMPORTANCE)
        );

        if (deleted > 0) {
            log.info("记忆条目定时清理完成，删除 {} 条过期记忆", deleted);
        }
        return deleted;
    }

    // ===================== 向量检索内部方法 =====================

    /**
     * 将记忆条目写入向量存储
     *
     * <p>使用 memoryEntry.id 作为 Document.id，便于检索后回查 DB。
     * metadata 保留 user_id / category / importance 用于过滤和展示。
     */
    private void indexToVectorStore(List<UserMemoryEntry> entries) {
        if (vectorStore == null || entries.isEmpty()) {
            return;
        }

        try {
            List<Document> documents = entries.stream()
                    .filter(e -> e.getId() != null)
                    .map(e -> {
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("user_id", e.getUserId());
                        metadata.put("memory_id", e.getId());
                        metadata.put("category", e.getCategory());
                        metadata.put("importance", e.getImportance());
                        metadata.put("source_type", e.getSourceType());

                        // PgVectorStore 要求 document id 必须是 UUID 格式
                        // 不能用数据库主键 id 的 toString()，必须生成真正的 UUID
                        String docId = UUID.randomUUID().toString();

                        return new Document(
                                docId,
                                e.getContent(),
                                metadata
                        );
                    })
                    .collect(Collectors.toList());

            if (!documents.isEmpty()) {
                vectorStore.add(documents);
                log.debug("记忆条目向量化完成: count={}", documents.size());
            }
        } catch (Exception e) {
            log.warn("记忆条目向量化失败（不影响主流程）: error={}", e.getMessage());
        }
    }

    /**
     * 向量语义检索记忆条目 ID
     *
     * @param userId 用户 ID
     * @param query  查询文本
     * @param topK   返回数量
     * @return 记忆条目 ID 列表（按相似度降序）
     */
    private List<Long> vectorSearch(Long userId, String query, int topK) {
        String filterExpression = "user_id == " + userId;

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(0.3)
                .filterExpression(filterExpression)
                .build();

        List<Document> documents = vectorStore.similaritySearch(request);

        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        return documents.stream()
                .map(doc -> {
                    Object memoryId = doc.getMetadata().get("memory_id");
                    if (memoryId instanceof Number) {
                        return ((Number) memoryId).longValue();
                    }
                    try {
                        return Long.parseLong((String) memoryId);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 根据 ID 列表查询记忆条目，保持向量检索的排序
     *
     * @param ids 记忆条目 ID 列表（按相似度降序）
     * @return 完整记忆条目列表
     */
    private List<UserMemoryEntry> fetchEntriesByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        // 批量查询
        List<UserMemoryEntry> entries = memoryEntryMapper.selectBatchIds(ids);

        // 按 ids 顺序排序（保持向量检索的相关性排序）
        Map<Long, UserMemoryEntry> entryMap = entries.stream()
                .collect(Collectors.toMap(UserMemoryEntry::getId, e -> e));

        return ids.stream()
                .map(entryMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
