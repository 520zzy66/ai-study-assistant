package com.study.ai.rag;

import com.study.entity.MaterialChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 用户学习资料向量索引服务。
 *
 * <p>数据库中的 {@code material_chunk} 是业务数据源，Spring AI
 * {@link VectorStore} 保存用于语义检索的副本。向量文档 ID 根据切片 ID
 * 确定性生成，使重试、移动文件夹和重新索引都具备幂等性。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialVectorIndexService {

    /** 用户资料在 VectorStore 中的来源标识。 */
    public static final String SOURCE_USER_MATERIAL = "user_material";

    private static final String DOCUMENT_ID_PREFIX = "material-chunk:";
    private static final int VECTOR_BATCH_SIZE = 100;

    private final VectorStore vectorStore;

    /**
     * 将资料切片写入向量库。
     *
     * @param chunks   已持久化且包含数据库 ID 的切片
     * @param fileType 原始资料类型
     */
    public void indexChunks(List<MaterialChunk> chunks, String fileType) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        List<Document> documents = new ArrayList<>(chunks.size());
        for (MaterialChunk chunk : chunks) {
            if (chunk.getId() == null) {
                throw new IllegalArgumentException("切片尚未持久化，无法建立向量索引");
            }
            documents.add(toDocument(chunk, fileType));
        }

        for (int start = 0; start < documents.size(); start += VECTOR_BATCH_SIZE) {
            vectorStore.add(documents.subList(start,
                    Math.min(start + VECTOR_BATCH_SIZE, documents.size())));
        }
        log.info("用户资料向量索引完成: materialId={}, chunks={}",
                chunks.get(0).getMaterialId(), documents.size());
    }

    /**
     * 从向量库删除指定切片。
     *
     * @param chunks 待删除的业务切片
     */
    public void deleteChunks(List<MaterialChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        List<String> documentIds = chunks.stream()
                .map(MaterialChunk::getId)
                .filter(java.util.Objects::nonNull)
                .map(MaterialVectorIndexService::documentId)
                .toList();
        if (!documentIds.isEmpty()) {
            for (int start = 0; start < documentIds.size(); start += VECTOR_BATCH_SIZE) {
                vectorStore.delete(documentIds.subList(start,
                        Math.min(start + VECTOR_BATCH_SIZE, documentIds.size())));
            }
            log.info("用户资料向量索引删除完成: chunks={}", documentIds.size());
        }
    }

    /**
     * 生成切片对应的稳定向量文档 ID。
     *
     * @param chunkId 数据库切片 ID
     * @return UUID 格式的向量文档 ID
     */
    public static String documentId(Long chunkId) {
        if (chunkId == null) {
            throw new IllegalArgumentException("chunkId 不能为空");
        }
        return UUID.nameUUIDFromBytes((DOCUMENT_ID_PREFIX + chunkId)
                .getBytes(StandardCharsets.UTF_8)).toString();
    }

    private Document toDocument(MaterialChunk chunk, String fileType) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", SOURCE_USER_MATERIAL);
        metadata.put("material_id", chunk.getMaterialId());
        metadata.put("user_id", chunk.getUserId());
        metadata.put("chunk_id", chunk.getId());
        metadata.put("chunk_index", chunk.getChunkIndex());
        if (chunk.getFolderId() != null) {
            metadata.put("folder_id", chunk.getFolderId());
        }
        if (fileType != null && !fileType.isBlank()) {
            metadata.put("file_type", fileType);
        }
        return new Document(documentId(chunk.getId()), chunk.getContent(), metadata);
    }
}
