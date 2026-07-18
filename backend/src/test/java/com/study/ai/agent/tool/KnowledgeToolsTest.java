package com.study.ai.agent.tool;

import com.study.ai.rag.HybridSearchService;
import com.study.ai.rag.TemporaryHybridSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.ToolParam;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeToolsTest {

    @Test
    void personalSearchUsesTrustedUserAndMaterialContext() {
        HybridSearchService hybridSearchService = mock(HybridSearchService.class);
        when(hybridSearchService.search(88L, 12L, null, "线代 特征值", 5)).thenReturn(java.util.List.of());
        KnowledgeTools tools = new KnowledgeTools(
                hybridSearchService, mock(TemporaryHybridSearchService.class), null);
        ToolContext context = new ToolContext(Map.of(
                KnowledgeTools.CONTEXT_USER_ID, 12L,
                KnowledgeTools.CONTEXT_MATERIAL_ID, 88L));

        tools.searchPersonalMaterial("特征值", "线代", context);

        verify(hybridSearchService).search(eq(88L), eq(12L), isNull(), eq("线代 特征值"), eq(5));
    }

    @Test
    void personalSearchScopesToFolderWhenFolderContextProvided() {
        HybridSearchService hybridSearchService = mock(HybridSearchService.class);
        when(hybridSearchService.search(null, 12L, 55L, "考公", 5)).thenReturn(java.util.List.of());
        KnowledgeTools tools = new KnowledgeTools(
                hybridSearchService, mock(TemporaryHybridSearchService.class), null);
        ToolContext context = new ToolContext(Map.of(
                KnowledgeTools.CONTEXT_USER_ID, 12L,
                KnowledgeTools.CONTEXT_FOLDER_ID, 55L));

        tools.searchPersonalMaterial("考公", null, context);

        verify(hybridSearchService).search(isNull(), eq(12L), eq(55L), eq("考公"), eq(5));
    }

    @Test
    void optionalToolArgumentsAreNotRequiredByGeneratedSchema() throws Exception {
        Method domainSearch = KnowledgeTools.class.getMethod(
                "searchDomainKnowledge", String.class, String.class, String.class);
        ToolParam folderName = domainSearch.getParameters()[2].getAnnotation(ToolParam.class);
        Method personalSearch = KnowledgeTools.class.getMethod(
                "searchPersonalMaterial", String.class, String.class, ToolContext.class);
        ToolParam quizType = personalSearch.getParameters()[1].getAnnotation(ToolParam.class);

        assertFalse(folderName.required());
        assertFalse(quizType.required());
    }

    @Test
    void temporarySearchUsesTrustedConversationScope() {
        HybridSearchService hybridSearchService = mock(HybridSearchService.class);
        TemporaryHybridSearchService temporarySearch = mock(TemporaryHybridSearchService.class);
        when(temporarySearch.search(12L, "conversation-1", "token-1", "图中公式", 5))
                .thenReturn(java.util.List.of());
        KnowledgeTools tools = new KnowledgeTools(hybridSearchService, temporarySearch, null);
        ToolContext context = new ToolContext(Map.of(
                KnowledgeTools.CONTEXT_USER_ID, 12L,
                KnowledgeTools.CONTEXT_CONVERSATION_ID, "conversation-1",
                KnowledgeTools.CONTEXT_TEMPORARY_MATERIAL_TOKEN, "token-1"));

        tools.searchTemporaryMaterial("图中公式", context);

        verify(temporarySearch).search(12L, "conversation-1", "token-1", "图中公式", 5);
    }
}
