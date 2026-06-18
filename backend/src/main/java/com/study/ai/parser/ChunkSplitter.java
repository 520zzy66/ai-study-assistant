package com.study.ai.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本切片器
 * 按段落优先策略切分，支持中文优化
 */
@Slf4j
@Component
public class ChunkSplitter {

    /** 每个切片的目标大小（字符数） */
    private static final int CHUNK_SIZE = 800;

    /** 切片之间的重叠字符数 */
    private static final int OVERLAP = 100;

    static {
        if (CHUNK_SIZE <= 0 || OVERLAP < 0) {
            throw new IllegalStateException("CHUNK_SIZE and OVERLAP must be positive");
        }
        if (OVERLAP >= CHUNK_SIZE) {
            throw new IllegalStateException("OVERLAP must be less than CHUNK_SIZE");
        }
    }

    /**
     * 将文本切分为多个片段
     *
     * @param text 原始文本
     * @return 切片列表
     */
    public List<String> split(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        // 1. 按段落切分（双换行或单换行）
        List<String> paragraphs = splitByParagraph(text);

        // 2. 合并过短段落，拆分过长段落
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String para : paragraphs) {
            // 当前段落加入后超过 chunkSize
            if (current.length() + para.length() > CHUNK_SIZE) {
                // 先保存当前内容
                if (current.length() > 0) {
                    chunks.add(current.toString().trim());
                    // 保留 overlap
                    current = new StringBuilder(lastNChars(current, OVERLAP));
                }

                // 处理超长段落：按 chunkSize 拆分
                if (para.length() > CHUNK_SIZE) {
                    List<String> subChunks = splitLongText(para);
                    for (int i = 0; i < subChunks.size(); i++) {
                        if (i == 0) {
                            // 第一段追加到当前（带 overlap 前缀）
                            current.append(subChunks.get(i));
                        } else {
                            chunks.add(subChunks.get(i));
                        }
                    }
                } else {
                    current.append(para);
                }
            } else {
                current.append(para);
            }
        }

        // 最后剩余内容
        if (current.length() > 0) {
            String last = current.toString().trim();
            if (!last.isEmpty()) {
                chunks.add(last);
            }
        }

        log.info("文本切片完成，总长度={}，切片数={}", text.length(), chunks.size());
        return chunks;
    }

    /**
     * 按段落切分文本
     */
    private List<String> splitByParagraph(String text) {
        List<String> paragraphs = new ArrayList<>();
        // 先按双换行切分，再按单换行切分
        String[] parts = text.split("\\n{2,}");
        for (String part : parts) {
            part = part.trim();
            if (!part.isEmpty()) {
                // 如果单个段落仍然很长，按句号等进一步切分
                if (part.length() > CHUNK_SIZE) {
                    paragraphs.addAll(splitBySentence(part));
                } else {
                    paragraphs.add(part + "\n");
                }
            }
        }
        return paragraphs;
    }

    /**
     * 按句子切分（中文句号、问号、感叹号、分号）
     */
    private List<String> splitBySentence(String text) {
        List<String> sentences = new ArrayList<>();
        // 按中文标点切分，保留标点
        String[] parts = text.split("(?<=[。！？；\\n])");
        for (String part : parts) {
            part = part.trim();
            if (!part.isEmpty()) {
                sentences.add(part);
            }
        }
        return sentences.isEmpty() ? List.of(text) : sentences;
    }

    /**
     * 切分超长文本
     */
    private List<String> splitLongText(String text) {
        List<String> result = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            result.add(text.substring(start, end));
            if (end >= text.length()) break;
            int nextStart = end - OVERLAP;
            // 防止死循环：确保 start 至少前进 1 个字符
            if (nextStart <= start) {
                start = end;
            } else {
                start = nextStart;
            }
        }
        return result;
    }

    /**
     * 获取 StringBuilder 末尾 N 个字符
     */
    private String lastNChars(StringBuilder sb, int n) {
        if (sb.length() <= n) {
            return sb.toString();
        }
        return sb.substring(sb.length() - n);
    }
}
