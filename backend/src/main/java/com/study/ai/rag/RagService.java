package com.study.ai.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.entity.MaterialChunk;
import com.study.mapper.MaterialChunkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    /** 相似度阈值（从配置读取，默认 0.3，关键词匹配场景下不宜过高） */
    @Value("${ai.rag.similarity-threshold:0.3}")
    private double similarityThreshold;

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

        // 3. 计算每个切片的匹配分数
        List<ChunkSearchResult> allResults = new ArrayList<>();
        for (MaterialChunk chunk : chunks) {
            double score = calculateKeywordScore(queryTokens, chunk.getContent());
            allResults.add(new ChunkSearchResult(
                    chunk.getId(),
                    chunk.getChunkIndex(),
                    chunk.getContent(),
                    score
            ));
        }

        // 4. 按分数降序排列
        allResults.sort(Comparator.comparing(ChunkSearchResult::getScore).reversed());

        // 5. 过滤超过阈值的结果
        List<ChunkSearchResult> results = allResults.stream()
                .filter(r -> r.getScore() > similarityThreshold)
                .limit(topK)
                .collect(Collectors.toList());

        // 6. 兜底：如果没有结果超过阈值，返回得分最高的 top-K（避免完全无结果）
        if (results.isEmpty() && !allResults.isEmpty()) {
            log.debug("无切片超过阈值 {}，返回得分最高的 {} 个切片（最高分={}）",
                    similarityThreshold, topK,
                    String.format("%.3f", allResults.get(0).getScore()));
            results = allResults.stream()
                    .limit(topK)
                    .collect(Collectors.toList());
        }

        return results;
    }

    /**
     * 计算关键词匹配分数
     * 多维度匹配：精确词匹配 + 中文字符匹配 + 位置权重
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

        double matchRatio = totalTokens > 0 ? (double) matched / totalTokens : 0;
        double positionWeight = matched > 0 ? positionBonus / matched : 0;

        // 额外计算中文字符覆盖率（解决分词粒度不一致问题）
        double charCoverage = calculateCharCoverage(queryTokens, docLower);

        // 综合得分：词匹配 40% + 字符覆盖 40% + 位置权重 20%
        return matchRatio * 0.4 + charCoverage * 0.4 + positionWeight * 0.2;
    }

    /**
     * 计算查询字符在文档中的覆盖率
     * 对中文文本，逐字检查是否出现在文档中，解决分词粒度不一致的问题
     */
    private double calculateCharCoverage(Set<String> queryTokens, String docLower) {
        // 提取查询中的所有中文字符
        Set<String> chineseChars = new LinkedHashSet<>();
        for (String token : queryTokens) {
            for (char c : token.toCharArray()) {
                if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                    chineseChars.add(String.valueOf(c));
                }
            }
        }
        if (chineseChars.isEmpty()) return 0;

        int covered = 0;
        for (String ch : chineseChars) {
            if (docLower.contains(ch)) {
                covered++;
            }
        }
        return (double) covered / chineseChars.size();
    }

    /**
     * 中文分词：单词 + 二元组 + 三元组 + 中文单字
     * 多粒度分词提高召回率
     */
    private Set<String> tokenize(String text) {
        Set<String> tokens = new LinkedHashSet<>();
        if (text == null || text.isBlank()) return tokens;

        // Remove punctuation, keep letters/digits/CJK
        String cleaned = PUNCT_PATTERN.matcher(text).replaceAll(" ").trim();
        String[] words = cleaned.split("\\s+");

        for (String word : words) {
            if (word.isEmpty()) continue;
            String lower = word.toLowerCase();
            tokens.add(lower);

            if (isChinese(word)) {
                // 中文单字（提高召回率）
                for (int i = 0; i < word.length(); i++) {
                    tokens.add(String.valueOf(word.charAt(i)));
                }
                // 二元组
                if (word.length() >= 2) {
                    for (int i = 0; i < word.length() - 1; i++) {
                        tokens.add(word.substring(i, i + 2));
                    }
                }
                // 三元组（捕获更长的语义单元）
                if (word.length() >= 3) {
                    for (int i = 0; i < word.length() - 2; i++) {
                        tokens.add(word.substring(i, i + 3));
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
