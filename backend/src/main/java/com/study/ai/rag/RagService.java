package com.study.ai.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.entity.MaterialChunk;
import com.study.mapper.MaterialChunkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * RAG 检索服务
 * 负责文档切片检索，支持关键词匹配 + 向量检索（向量检索待 Embedding 模型集成后启用）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final MaterialChunkMapper chunkMapper;

    /** 相似度阈值 */
    private static final double SIMILARITY_THRESHOLD = 0.1;

    /** 默认 Top-K */
    private static final int DEFAULT_TOP_K = 5;

    /** 单次检索最大加载切片数，防止超大文档导致内存压力 */
    private static final int MAX_CHUNKS_PER_SEARCH = 200;

    /**
     * Remove punctuation: ASCII punct + whitespace + CJK punctuation chars.
     * Uses Unicode escapes to avoid IDE smart-quote corruption.
     * Chars: “” ‘’ (smart quotes), ，。！？；： (CJK punct),
     * 【】（）《》 (CJK brackets)
     */
    private static final Pattern PUNCT_PATTERN = Pattern.compile(
            "[\\p{Punct}\\s"
                    + "“”‘’"
                    + "，。！？；："
                    + "【】（）《》"
                    + "]+");

    /**
     * 检索最相关的切片
     * 当前使用关键词匹配（TF-IDF 风格），后续可升级为混合检索
     *
     * @param materialId 资料ID
     * @param query      查询问题
     * @param topK       返回结果数
     * @return 搜索结果列表
     */
    public List<ChunkSearchResult> search(Long materialId, String query, int topK) {
        if (topK <= 0) topK = DEFAULT_TOP_K;

        // 1. 加载该资料的切片（限制最大数量防止内存压力）
        List<MaterialChunk> chunks = chunkMapper.selectList(
                new LambdaQueryWrapper<MaterialChunk>()
                        .eq(MaterialChunk::getMaterialId, materialId)
                        .orderByAsc(MaterialChunk::getChunkIndex)
                        .last("LIMIT " + MAX_CHUNKS_PER_SEARCH)
        );

        if (chunks.size() >= MAX_CHUNKS_PER_SEARCH) {
            log.warn("资料 {} 切片数超过上限 {}，仅检索前 {} 个切片",
                    materialId, MAX_CHUNKS_PER_SEARCH, MAX_CHUNKS_PER_SEARCH);
        }

        if (chunks.isEmpty()) {
            return List.of();
        }

        // 2. 对查询分词
        Set<String> queryTokens = tokenize(query);

        // 3. 计算每个切片的匹配分数（简单 TF-IDF 风格关键词匹配）
        List<ChunkSearchResult> results = new ArrayList<>();
        for (MaterialChunk chunk : chunks) {
            double score = calculateKeywordScore(queryTokens, chunk.getContent());
            if (score > SIMILARITY_THRESHOLD) {
                results.add(new ChunkSearchResult(
                        chunk.getId(),
                        chunk.getChunkIndex(),
                        chunk.getContent(),
                        score
                ));
            }
        }

        // 4. 按分数降序排列，取 Top-K
        return results.stream()
                .sorted(Comparator.comparing(ChunkSearchResult::getScore).reversed())
                .limit(topK)
                .collect(Collectors.toList());
    }

    /**
     * 计算关键词匹配分数
     * 简单 TF-IDF 风格：查询词在文档中出现的频率 + 位置权重（靠前权重更高）
     */
    private double calculateKeywordScore(Set<String> queryTokens, String document) {
        if (queryTokens.isEmpty() || document == null) return 0;

        String docLower = document.toLowerCase();
        int totalTokens = queryTokens.size();
        int matched = 0;
        double positionBonus = 0;

        for (String token : queryTokens) {
            int idx = docLower.indexOf(token);
            if (idx >= 0) {
                matched++;
                positionBonus += 1.0 / (1 + idx / 100.0);
            }
        }

        double matchRatio = (double) matched / totalTokens;
        double positionWeight = matched > 0 ? positionBonus / matched : 0;

        return matchRatio * 0.7 + positionWeight * 0.3;
    }

    /**
     * 简单中文分词（基于字符二元组 + 关键词拆分）
     * 后续可升级为 jieba 分词
     */
    private Set<String> tokenize(String text) {
        Set<String> tokens = new LinkedHashSet<>();
        if (text == null || text.isBlank()) return tokens;

        // Remove punctuation, keep letters/digits/CJK
        String cleaned = PUNCT_PATTERN.matcher(text).replaceAll(" ").trim();
        String[] words = cleaned.split("\\s+");

        for (String word : words) {
            if (word.length() >= 1) {
                tokens.add(word.toLowerCase());
                // Generate bigrams for CJK text
                if (word.length() >= 2 && isChinese(word)) {
                    for (int i = 0; i < word.length() - 1; i++) {
                        tokens.add(word.substring(i, i + 2));
                    }
                }
            }
        }

        return tokens;
    }

    /**
     * 判断是否为纯中文字符串
     */
    private boolean isChinese(String s) {
        for (char c : s.toCharArray()) {
            if (Character.UnicodeScript.of(c) != Character.UnicodeScript.HAN) {
                return false;
            }
        }
        return true;
    }
}
