package com.study.ai.rag;

import com.huaban.analysis.jieba.JiebaSegmenter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * BM25 关键词检索服务
 * 基于 jieba 中文分词 + 倒排索引 + BM25 评分算法
 *
 * <p>BM25 公式：score(D,Q) = Σ IDF(qi) × [tf(qi,D) × (k1+1)] / [tf(qi,D) + k1 × (1 - b + b × |D|/avgdl)]
 *
 * <p>参数说明：
 * <ul>
 *   <li>k1 = 1.5：控制词频饱和度（越大越不饱和）</li>
 *   <li>b = 0.75：控制文档长度归一化强度（0 = 不归一，1 = 完全归一）</li>
 * </ul>
 *
 * <p>使用方式：每次检索时传入切片列表，服务内部构建倒排索引并计算分数。
 * 索引不持久化（数据量小，内存计算即可）。
 */
@Slf4j
@Service
public class Bm25Service {

    /** BM25 参数 k1：控制词频饱和速度 */
    private static final double K1 = 1.5;

    /** BM25 参数 b：控制文档长度归一化（0.75 为经验最优值） */
    private static final double B = 0.75;

    /** jieba 分词器（线程安全，可复用） */
    private final JiebaSegmenter segmenter = new JiebaSegmenter();

    /** 中文停用词集合 */
    private static final Set<String> STOP_WORDS = Set.of(
            // 助词/虚词
            "的", "了", "着", "过", "是", "在", "有", "和", "与", "或",
            "但", "而", "就", "也", "都", "不", "没", "很", "非常",
            "能", "会", "要", "让", "把", "被", "给", "从", "到", "对",
            "为", "以", "及", "等", "之", "其", "此", "那", "个", "们",
            "来", "去", "说", "还", "该", "吗", "呢", "吧", "啊", "嗯",
            "哈", "呀", "哦", "唉", "么", "哪", "什么", "怎么", "如何",
            "为什么", "多少", "几", "谁", "它", "他", "她", "我", "你",
            "您", "大", "小", "多", "少", "好", "坏", "高", "低", "长",
            "短", "上", "下", "中", "内", "外", "里", "后", "前", "时"
    );

    /**
     * 对查询文本进行 BM25 检索
     *
     * @param chunks  切片列表（id, content）
     * @param query   查询文本
     * @param topK    返回前 K 个结果
     * @return 按 BM25 分数降序排列的搜索结果
     */
    public List<ChunkSearchResult> search(List<ScoredChunk> chunks, String query, int topK) {
        if (chunks == null || chunks.isEmpty() || query == null || query.isBlank()) {
            return List.of();
        }

        // 1. 对所有切片分词，构建倒排索引
        Map<String, List<Integer>> invertedIndex = new HashMap<>(); // term -> chunk indices
        List<List<String>> chunkTokens = new ArrayList<>();
        double totalLength = 0;

        for (int i = 0; i < chunks.size(); i++) {
            ScoredChunk chunk = chunks.get(i);
            List<String> tokens = tokenize(chunk.content());
            chunkTokens.add(tokens);
            totalLength += tokens.size();

            // 构建倒排索引（去重，每个词在一个切片中只记录一次用于 IDF）
            Set<String> uniqueTokens = new HashSet<>(tokens);
            for (String token : uniqueTokens) {
                invertedIndex.computeIfAbsent(token, k -> new ArrayList<>()).add(i);
            }
        }

        int docCount = chunks.size();
        double avgDocLength = totalLength / docCount;

        // 2. 对查询分词
        List<String> queryTokens = tokenize(query);
        if (queryTokens.isEmpty()) {
            return List.of();
        }

        // 3. 计算每个切片的 BM25 分数
        double[] scores = new double[docCount];

        for (String term : queryTokens) {
            List<Integer> postings = invertedIndex.get(term);
            if (postings == null) continue;

            // IDF 计算：log((N - n + 0.5) / (n + 0.5) + 1)
            double idf = Math.log((docCount - postings.size() + 0.5) / (postings.size() + 0.5) + 1.0);

            for (int docIndex : postings) {
                // 词频：该 term 在该切片中出现次数
                int tf = countFrequency(chunkTokens.get(docIndex), term);
                double docLen = chunkTokens.get(docIndex).size();

                // BM25 核心公式
                double numerator = tf * (K1 + 1);
                double denominator = tf + K1 * (1 - B + B * docLen / avgDocLength);
                scores[docIndex] += idf * numerator / denominator;
            }
        }

        // 4. 构建结果列表并排序
        List<ChunkSearchResult> results = new ArrayList<>();
        for (int i = 0; i < docCount; i++) {
            if (scores[i] > 0) {
                ScoredChunk chunk = chunks.get(i);
                results.add(new ChunkSearchResult(
                        chunk.id(),
                        chunk.chunkIndex(),
                        chunk.content(),
                        scores[i]
                ));
            }
        }

        results.sort(Comparator.comparing(ChunkSearchResult::getScore).reversed());

        // 5. 截取 Top-K
        if (results.size() > topK) {
            results = results.subList(0, topK);
        }

        log.debug("BM25 检索完成：queryTokens={}, docCount={}, results={}",
                queryTokens, docCount, results.size());

        return results;
    }

    /**
     * jieba 中文分词 + 停用词过滤
     *
     * @param text 输入文本
     * @return 过滤后的词元列表（小写）
     */
    public List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        return segmenter.sentenceProcess(text).stream()
                .map(String::toLowerCase)
                .filter(token -> token.length() > 1)           // 过滤单字（噪声大）
                .filter(token -> !STOP_WORDS.contains(token))  // 过滤停用词
                .filter(token -> !isNumeric(token))            // 过滤纯数字
                .collect(Collectors.toList());
    }

    /**
     * 统计词在 token 列表中的出现频率
     */
    private int countFrequency(List<String> tokens, String term) {
        int count = 0;
        for (String token : tokens) {
            if (token.equals(term)) count++;
        }
        return count;
    }

    /**
     * 判断字符串是否为纯数字
     */
    private boolean isNumeric(String s) {
        for (char c : s.toCharArray()) {
            if (!Character.isDigit(c)) return false;
        }
        return !s.isEmpty();
    }

    /**
     * BM25 检索用的切片数据载体（避免依赖 MaterialChunk Entity）
     */
    public record ScoredChunk(Long id, Integer chunkIndex, String content) {
    }
}
