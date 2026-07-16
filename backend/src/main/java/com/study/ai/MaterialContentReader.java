package com.study.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.entity.LearningMaterial;
import com.study.entity.MaterialChunk;
import com.study.mapper.LearningMaterialMapper;
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
    private final LearningMaterialMapper materialMapper;

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

    /**
     * 加载文件夹下所有资料的切片内容并拼接
     * 从向量数据库读取该文件夹下所有内容
     *
     * @param folderId 文件夹ID
     * @param userId   用户ID
     * @return 拼接后的文本
     */
    public String readFolderContent(Long folderId, Long userId) {
        return readFolderContent(folderId, userId, MAX_CONTENT_LEN);
    }

    /**
     * 加载文件夹下所有资料的切片内容并指定最大长度
     *
     * @param folderId 文件夹ID
     * @param userId   用户ID
     * @param maxLen   最大长度（字符）
     * @return 拼接后的文本
     */
    public String readFolderContent(Long folderId, Long userId, int maxLen) {
        // 1. 查询文件夹下的所有资料
        List<LearningMaterial> materials = materialMapper.selectList(
                new LambdaQueryWrapper<LearningMaterial>()
                        .eq(LearningMaterial::getFolderId, folderId)
                        .eq(LearningMaterial::getUserId, userId)
                        .eq(LearningMaterial::getStatus, "ready")
        );

        if (materials.isEmpty()) {
            log.warn("文件夹下没有可用资料: folderId={}", folderId);
            return "";
        }

        // 2. 获取所有资料ID
        List<Long> materialIds = materials.stream()
                .map(LearningMaterial::getId)
                .collect(Collectors.toList());

        // 3. 查询所有切片内容（按资料ID和切片索引排序）
        List<MaterialChunk> chunks = chunkMapper.selectList(
                new LambdaQueryWrapper<MaterialChunk>()
                        .in(MaterialChunk::getMaterialId, materialIds)
                        .orderByAsc(MaterialChunk::getMaterialId)
                        .orderByAsc(MaterialChunk::getChunkIndex)
        );

        // 4. 拼接内容
        StringBuilder sb = new StringBuilder();
        for (MaterialChunk chunk : chunks) {
            sb.append(chunk.getContent()).append("\n\n");
            if (sb.length() > maxLen) {
                sb.append("[注：文档内容过长，已截断...]\n");
                break;
            }
        }

        String result = sb.toString();
        log.debug("文件夹内容读取: folderId={}, materials={}, chunks={}, len={}",
                folderId, materials.size(), chunks.size(), result.length());
        return result;
    }

    /**
     * 获取文件夹下所有资料的ID列表
     *
     * @param folderId 文件夹ID
     * @param userId   用户ID
     * @return 资料ID列表
     */
    public List<Long> getFolderMaterialIds(Long folderId, Long userId) {
        List<LearningMaterial> materials = materialMapper.selectList(
                new LambdaQueryWrapper<LearningMaterial>()
                        .eq(LearningMaterial::getFolderId, folderId)
                        .eq(LearningMaterial::getUserId, userId)
                        .eq(LearningMaterial::getStatus, "ready")
        );
        return materials.stream()
                .map(LearningMaterial::getId)
                .collect(Collectors.toList());
    }
}
