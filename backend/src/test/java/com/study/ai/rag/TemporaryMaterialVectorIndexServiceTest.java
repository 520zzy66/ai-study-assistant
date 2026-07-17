package com.study.ai.rag;

import com.study.entity.TemporaryMaterialChunk;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TemporaryMaterialVectorIndexServiceTest {

    @Test
    void writesAllIsolationMetadataAndStableId() {
        VectorStore vectorStore = mock(VectorStore.class);
        TemporaryMaterialVectorIndexService service = new TemporaryMaterialVectorIndexService(vectorStore);
        TemporaryMaterialChunk chunk = new TemporaryMaterialChunk();
        chunk.setId(31L);
        chunk.setTemporaryMaterialId(8L);
        chunk.setUploadToken("token-1");
        chunk.setUserId(12L);
        chunk.setConversationId("conversation-1");
        chunk.setChunkIndex(0);
        chunk.setContent("图片中的公式");

        service.indexChunks(List.of(chunk), "png");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());
        Document document = captor.getValue().getFirst();
        assertEquals(TemporaryMaterialVectorIndexService.documentId(31L), document.getId());
        assertEquals(12L, document.getMetadata().get("user_id"));
        assertEquals("conversation-1", document.getMetadata().get("conversation_id"));
        assertEquals("token-1", document.getMetadata().get("upload_token"));
        assertEquals("temporary_material", document.getMetadata().get("source"));
    }
}
