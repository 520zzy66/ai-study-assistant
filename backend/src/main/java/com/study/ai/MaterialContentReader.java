package com.study.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.entity.MaterialChunk;
import com.study.mapper.MaterialChunkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 资料内容读取器
 * 统一加载资料的切片内容并拼接，供 AiSummaryService / AiQuizService 复用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaterialContentReader {

    private final MaterialChunkMapper chunkMapper;

    /** 内容最大长度（字符），超出截断 */
    private static final int MAX_CONTENT_LEN = 16000;

    /**
     * 加载资料的所有切片内容并拼接为完整文本
     *
     * @param materialId 资料ID
     * @return 拼接后的文本（超过 MAX_CONTENT_LEN 时自动截断并追加提示）
     */
    public String readContent(Long materialId) {
        return readContent(materialId, MAX_CONTENT_LEN);
    }

    /**
     * 加载资料内容并指定最大长度
     *
     * @param materialId 资料ID
     * @param maxLen     最大长度（字符）
     * @return 拼接后的文本
     */
    public String readContent(Long materialId, int maxLen) {
        List<MaterialChunk> chunks = chunkMapper.selectList(
                new LambdaQueryWrapper<MaterialChunk>()
                        .eq(MaterialChunk::getMaterialId, materialId)
                        .orderByAsc(MaterialChunk::getChunkIndex)
        );

        StringBuilder sb = new StringBuilder();
        for (MaterialChunk chunk : chunks) {
            sb.append(chunk.getContent()).append("\n\n");
            if (sb.length() > maxLen) {
                sb.append("[注：文档内容过长，已截断...]\n");
                break;
            }
        }

        String result = sb.toString();
        log.debug("资料内容读取: materialId={}, chunks={}, len={}", materialId, chunks.size(), result.length());
        return result;
    }
}
