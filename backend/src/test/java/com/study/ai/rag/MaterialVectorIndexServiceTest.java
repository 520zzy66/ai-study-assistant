package com.study.ai.rag;

import com.study.entity.MaterialChunk;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MaterialVectorIndexServiceTest {

    @Test
    void indexesPersistedChunkWithStableIdAndIsolationMetadata() {
        VectorStore vectorStore = mock(VectorStore.class);
        MaterialVectorIndexService service = new MaterialVectorIndexService(vectorStore);
        MaterialChunk chunk = chunk(21L, 7L, 9L, 3L);

        service.indexChunks(List.of(chunk), "pdf");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());
        Document document = captor.getValue().getFirst();
        assertEquals(MaterialVectorIndexService.documentId(21L), document.getId());
        assertEquals(7L, document.getMetadata().get("material_id"));
        assertEquals(9L, document.getMetadata().get("user_id"));
        assertEquals(3L, document.getMetadata().get("folder_id"));
        assertEquals("user_material", document.getMetadata().get("source"));
    }

    @Test
    void deletesUsingTheSameStableDocumentId() {
        VectorStore vectorStore = mock(VectorStore.class);
        MaterialVectorIndexService service = new MaterialVectorIndexService(vectorStore);

        service.deleteChunks(List.of(chunk(21L, 7L, 9L, null)));

        verify(vectorStore).delete(List.of(MaterialVectorIndexService.documentId(21L)));
    }

    private MaterialChunk chunk(Long id, Long materialId, Long userId, Long folderId) {
        MaterialChunk chunk = new MaterialChunk();
        chunk.setId(id);
        chunk.setMaterialId(materialId);
        chunk.setUserId(userId);
        chunk.setFolderId(folderId);
        chunk.setChunkIndex(0);
        chunk.setContent("测试切片");
        return chunk;
    }
}
