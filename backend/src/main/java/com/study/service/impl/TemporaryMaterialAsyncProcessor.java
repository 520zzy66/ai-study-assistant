package com.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.study.ai.parser.ChunkSplitter;
import com.study.ai.parser.TemporaryMaterialContentExtractor;
import com.study.ai.rag.TemporaryMaterialVectorIndexService;
import com.study.entity.TemporaryMaterial;
import com.study.entity.TemporaryMaterialChunk;
import com.study.mapper.TemporaryMaterialChunkMapper;
import com.study.mapper.TemporaryMaterialMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 临时资料异步解析与索引处理器。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemporaryMaterialAsyncProcessor {

    private final TemporaryMaterialMapper materialMapper;
    private final TemporaryMaterialChunkMapper chunkMapper;
    private final TemporaryMaterialContentExtractor contentExtractor;
    private final ChunkSplitter chunkSplitter;
    private final TemporaryMaterialVectorIndexService vectorIndexService;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    /**
     * 异步解析、切片并写入临时向量索引。
     */
    @Async("taskExecutor")
    public void process(Long temporaryMaterialId) {
        TemporaryMaterial material = materialMapper.selectById(temporaryMaterialId);
        if (material == null || !"processing".equals(material.getStatus())) {
            return;
        }
        try {
            Path fullPath = Paths.get(uploadDir, material.getFilePath()).normalize();
            String text = contentExtractor.extract(fullPath, material.getFileType());
            List<String> contentChunks = chunkSplitter.split(text);
            if (contentChunks.isEmpty()) {
                throw new IllegalStateException("资料内容为空，无法建立临时索引");
            }

            List<TemporaryMaterialChunk> chunks = new ArrayList<>(contentChunks.size());
            for (int i = 0; i < contentChunks.size(); i++) {
                TemporaryMaterialChunk chunk = new TemporaryMaterialChunk();
                chunk.setTemporaryMaterialId(material.getId());
                chunk.setUploadToken(material.getUploadToken());
                chunk.setUserId(material.getUserId());
                chunk.setConversationId(material.getConversationId());
                chunk.setChunkIndex(i);
                chunk.setContent(contentChunks.get(i));
                chunk.setChunkSize(contentChunks.get(i).length());
                chunks.add(chunk);
            }
            Db.saveBatch(chunks, 100);
            List<TemporaryMaterialChunk> persistedChunks = chunkMapper.selectList(
                    new LambdaQueryWrapper<TemporaryMaterialChunk>()
                            .eq(TemporaryMaterialChunk::getTemporaryMaterialId, material.getId())
                            .orderByAsc(TemporaryMaterialChunk::getChunkIndex));
            if (persistedChunks.size() != contentChunks.size()) {
                throw new IllegalStateException("临时资料切片保存不完整");
            }

            int claimed = materialMapper.update(null, new LambdaUpdateWrapper<TemporaryMaterial>()
                    .eq(TemporaryMaterial::getId, material.getId())
                    .eq(TemporaryMaterial::getStatus, "processing")
                    .set(TemporaryMaterial::getStatus, "indexing"));
            if (claimed == 0) {
                cleanupPartialChunks(material.getId());
                return;
            }
            vectorIndexService.indexChunks(persistedChunks, material.getFileType());

            TemporaryMaterial update = new TemporaryMaterial();
            update.setId(material.getId());
            update.setStatus("ready");
            update.setChunkCount(persistedChunks.size());
            update.setErrorMsg(null);
            materialMapper.update(update, new LambdaUpdateWrapper<TemporaryMaterial>()
                    .eq(TemporaryMaterial::getId, material.getId())
                    .eq(TemporaryMaterial::getStatus, "indexing"));
            log.info("临时资料处理完成: token={}, chunks={}", material.getUploadToken(), chunks.size());
        } catch (Exception e) {
            log.error("临时资料处理失败: token={}", material.getUploadToken(), e);
            cleanupPartialChunks(material.getId());
            TemporaryMaterial update = new TemporaryMaterial();
            update.setId(material.getId());
            update.setStatus("failed");
            String message = e.getMessage() == null ? "未知错误" : e.getMessage();
            update.setErrorMsg(message.length() > 500 ? message.substring(0, 500) : message);
            materialMapper.updateById(update);
        }
    }

    private void cleanupPartialChunks(Long temporaryMaterialId) {
        List<TemporaryMaterialChunk> chunks = chunkMapper.selectList(
                new LambdaQueryWrapper<TemporaryMaterialChunk>()
                        .eq(TemporaryMaterialChunk::getTemporaryMaterialId, temporaryMaterialId));
        try {
            vectorIndexService.deleteChunks(chunks);
        } catch (Exception e) {
            log.warn("清理临时资料部分向量失败: materialId={}", temporaryMaterialId, e);
        }
        chunkMapper.delete(new LambdaQueryWrapper<TemporaryMaterialChunk>()
                .eq(TemporaryMaterialChunk::getTemporaryMaterialId, temporaryMaterialId));
    }
}
