package com.study.ai.rag;

import com.study.entity.TemporaryMaterialChunk;
import lombok.RequiredArgsConstructor;
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
 * 会话临时资料向量索引服务。
 */
@Service
@RequiredArgsConstructor
public class TemporaryMaterialVectorIndexService {

    public static final String SOURCE_TEMPORARY_MATERIAL = "temporary_material";
    private static final String ID_PREFIX = "temporary-material-chunk:";
    private static final int VECTOR_BATCH_SIZE = 100;

    private final VectorStore vectorStore;

    /**
     * 写入临时资料切片向量。
     *
     * @param chunks 已持久化切片
     * @param fileType 文件类型
     */
    public void indexChunks(List<TemporaryMaterialChunk> chunks, String fileType) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        List<Document> documents = new ArrayList<>(chunks.size());
        for (TemporaryMaterialChunk chunk : chunks) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", SOURCE_TEMPORARY_MATERIAL);
            metadata.put("temporary_material_id", chunk.getTemporaryMaterialId());
            metadata.put("upload_token", chunk.getUploadToken());
            metadata.put("user_id", chunk.getUserId());
            metadata.put("conversation_id", chunk.getConversationId());
            metadata.put("chunk_id", chunk.getId());
            metadata.put("chunk_index", chunk.getChunkIndex());
            metadata.put("file_type", fileType);
            documents.add(new Document(documentId(chunk.getId()), chunk.getContent(), metadata));
        }
        for (int start = 0; start < documents.size(); start += VECTOR_BATCH_SIZE) {
            vectorStore.add(documents.subList(start,
                    Math.min(start + VECTOR_BATCH_SIZE, documents.size())));
        }
    }

    /**
     * 删除临时资料切片向量。
     *
     * @param chunks 待删除切片
     */
    public void deleteChunks(List<TemporaryMaterialChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        List<String> ids = chunks.stream()
                .map(TemporaryMaterialChunk::getId)
                .filter(java.util.Objects::nonNull)
                .map(TemporaryMaterialVectorIndexService::documentId)
                .toList();
        for (int start = 0; start < ids.size(); start += VECTOR_BATCH_SIZE) {
            vectorStore.delete(ids.subList(start, Math.min(start + VECTOR_BATCH_SIZE, ids.size())));
        }
    }

    /**
     * 生成稳定向量文档 ID。
     *
     * @param chunkId 临时切片 ID
     * @return UUID 字符串
     */
    public static String documentId(Long chunkId) {
        return UUID.nameUUIDFromBytes((ID_PREFIX + chunkId)
                .getBytes(StandardCharsets.UTF_8)).toString();
    }
}
