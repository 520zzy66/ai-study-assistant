package com.study.ai.workflow.service;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.study.ai.MaterialContentReader;
import com.study.ai.agent.config.AgentClientFactory;
import com.study.ai.agent.tool.KnowledgeTools;
import com.study.ai.workflow.graph.RouteKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared Spring AI ChatClient implementation for expert agents.
 *
 * <p>System prompt is loaded from agent-{id}.yml → system-prompt-file (e.g. civil-system.txt).
 * User prompt built here follows the 5-class input structure defined in those system prompts:
 * <ol>
 *   <li>用户原始问题</li>
 *   <li>路由总结（routingSummary）</li>
 *   <li>资料向量检索结果（materialText 摘录 + historyChatChunks）</li>
 *   <li>领域知识库检索结果（通过 KnowledgeTools 按需检索）</li>
 *   <li>用户画像与记忆（compressedProfile + memoryContext）</li>
 * </ol>
 */
@Slf4j
public abstract class AbstractExpertAgentService implements ExpertAgentService {

    private static final int MAX_MATERIAL_CHARS = 2400;
    private static final int MAX_CONTEXT_CHARS = 1600;

    private final AgentClientFactory agentClientFactory;
    private final KnowledgeTools knowledgeTools;
    private final MaterialContentReader materialContentReader;
    private final String agentId;
    private final String expertId;

    protected AbstractExpertAgentService(AgentClientFactory agentClientFactory,
                                         KnowledgeTools knowledgeTools,
                                         MaterialContentReader materialContentReader,
                                         String agentId,
                                         String expertId) {
        this.agentClientFactory = agentClientFactory;
        this.knowledgeTools = knowledgeTools;
        this.materialContentReader = materialContentReader;
        this.agentId = agentId;
        this.expertId = expertId;
    }

    @Override
    public String getExpertId() {
        return expertId;
    }

    @Override
    public String answer(OverAllState state) {
        ChatClient client = agentClientFactory.getOrCreate(agentId);
        String prompt = buildPrompt(state);

        // Spring AI 自动 ReAct：LLM 自主决定调用哪些 Tool
        String answer = client.prompt()
                .user(prompt)
                .toolContext(buildToolContext(state))
                .tools(knowledgeTools)
                .call()
                .content();

        if (answer == null || answer.isBlank()) {
            return "暂时没有生成有效回答，请补充问题细节后再试。";
        }
        return answer;
    }

    @Override
    public Flux<String> answerStream(OverAllState state) {
        ChatClient client = agentClientFactory.getOrCreate(agentId);
        String prompt = buildPrompt(state);

        // Spring AI 流式调用
        return client.prompt()
                .user(prompt)
                .toolContext(buildToolContext(state))
                .tools(knowledgeTools)
                .stream()
                .content()
                .filter(content -> content != null && !content.isBlank());
    }

    /**
     * 构建用户提示词，对齐 system prompt（civil-system.txt / graduate-system.txt）定义的输入结构。
     *
     * <p>system prompt 定义了 5 类输入：
     * <ol>
     *   <li>用户原始问题</li>
     *   <li>路由总结（routingSummary）</li>
     *   <li>资料向量检索结果 — 用户上传资料的相关片段</li>
     *   <li>领域知识库检索结果 — 通过 KnowledgeTools 按需检索</li>
     *   <li>用户画像与记忆</li>
     * </ol>
     *
     * <p>folderId 存在时的增强引导（避免 LLM 误用系统知识库结构回答用户文件夹相关问题）：
     * <ul>
     *   <li>显式告知 AI 当前会话关联了整个文件夹，但尚未预检索内容</li>
     *   <li>当用户请求"总结/概括/梳理"整个文件夹时，强制先调用 searchPersonalMaterial 检索实际切片</li>
     *   <li>禁止仅基于元信息或系统知识库结构描述用户文件夹内容</li>
     * </ul>
     */
    private String buildPrompt(OverAllState state) {
        StringBuilder sb = new StringBuilder();

        // ========== 1. 用户原始问题 ==========
        String query = state.value(RouteKeys.QUERY, "");
        sb.append("## 1. 用户原始问题\n");
        sb.append(query).append("\n\n");

        // ========== 2. 路由总结 ==========
        String routingSummary = state.value(RouteKeys.ROUTING_SUMMARY, "");
        if (!routingSummary.isBlank()) {
            sb.append("## 2. 路由总结\n");
            sb.append(truncate(routingSummary, MAX_CONTEXT_CHARS)).append("\n\n");
        }

        // ========== 3. 资料向量检索结果 ==========
        // 3a. 资料原文摘录（来自用户上传的当前资料）
        String materialText = state.value(RouteKeys.MATERIAL_TEXT, "");
        String materialSummary = state.value(RouteKeys.MATERIAL_SUMMARY, "");
        String materialMeta = state.value(RouteKeys.MATERIAL_META, "");
        Long folderId = parseLongFromState(state.value(RouteKeys.FOLDER_ID));
        Long materialId = parseLongFromState(state.value(RouteKeys.MATERIAL_ID));

        boolean hasFolder = folderId != null;
        boolean hasMaterial = materialId != null;
        if (hasFolder) {
            String folderContent = materialContentReader.readFolderContent(
                    folderId,
                    parseLongFromState(state.value(RouteKeys.USER_ID)),
                    10000);
            sb.append("## 3. 用户文件夹真实资料内容\n");
            if (folderContent != null && !folderContent.isBlank()) {
                sb.append(folderContent).append("\n\n");
            } else {
                sb.append("文件夹下暂无可用的已处理资料切片。\n\n");
            }
        } else if (!materialText.isBlank() || !materialSummary.isBlank() || !materialMeta.isBlank()) {
            sb.append("## 3. 资料向量检索结果\n");
            if (!materialSummary.isBlank()) {
                sb.append("**资料描述**：").append(truncate(materialSummary, 800)).append("\n\n");
            }
            if (!materialMeta.isBlank()) {
                sb.append("**资料元信息**：").append(truncate(materialMeta, 400)).append("\n\n");
            }
            if (!materialText.isBlank()) {
                sb.append("**资料摘录**：\n").append(truncate(materialText, MAX_MATERIAL_CHARS)).append("\n\n");
            }
        } else if (hasMaterial) {
            sb.append("## 3. 资料向量检索结果\n");
            sb.append("**当前会话关联了一份资料**（materialId=").append(materialId).append("），");
            sb.append("尚未预检索切片内容。如需资料内容请先调用 searchPersonalMaterial 工具。\n\n");
        }
        // 3b. 语义检索的历史对话片段
        appendHistoryChunks(sb, state.value(RouteKeys.HISTORY_CHUNKS));

        // ========== 4. 用户画像与记忆 ==========
        String compressedProfile = state.value(RouteKeys.COMPRESSED_PROFILE, "");
        String memoryContext = state.value(RouteKeys.MEMORY_CONTEXT, "");
        if (!compressedProfile.isBlank() || !memoryContext.isBlank()) {
            sb.append("## 4. 用户画像与记忆\n");
            if (!compressedProfile.isBlank()) {
                sb.append("**学习画像**：").append(truncate(compressedProfile, 800)).append("\n\n");
            }
            if (!memoryContext.isBlank()) {
                sb.append("**长期记忆**：\n").append(truncate(memoryContext, 800)).append("\n\n");
            }
        }

        // ========== 5. 近期对话 ==========
        String recentContext = state.value(RouteKeys.RECENT_CONTEXT, "");
        if (!recentContext.isBlank()) {
            sb.append("## 5. 近期对话\n");
            sb.append(truncate(recentContext, MAX_CONTEXT_CHARS)).append("\n\n");
        }

        // ========== 执行指引 ==========
        sb.append("## 执行指引\n");
        sb.append("- 领域知识库检索结果需通过工具获取：searchDomainKnowledge(domain, query, folderName?)\n");
        sb.append("- 用户资料补充检索：searchPersonalMaterial(query, quizType?)\n");
        sb.append("- 当前会话临时资料检索：searchTemporaryMaterial(query)\n");
        sb.append("- 历史对话补充检索：searchConversationHistory(query)\n");
        if (hasFolder) {
            // 关键引导：folderId 存在时强制要求"总结/概括/梳理"类指令必须先检索文件夹内容
            sb.append("- **当前会话关联了整个文件夹**：当用户请求「总结」「概括」「梳理」「整理」");
            sb.append("「归纳」整个文件夹或其中内容时，**必须先调用 searchPersonalMaterial 工具");
            sb.append("检索文件夹实际切片**，可针对不同主题/章节/知识点发起多次检索，");
            sb.append("再基于检索到的实际内容生成总结。\n");
            sb.append("- **严禁**仅基于元信息或系统知识库（searchDomainKnowledge 返回的 folderName）");
            sb.append("描述用户文件夹内容；用户文件夹的实际内容只能通过 searchPersonalMaterial 获取。\n");
            sb.append("- 若 searchPersonalMaterial 多次检索后仍无结果，明确告知用户");
            sb.append("「文件夹下暂无可用资料切片」，不要编造文件结构。\n");
        } else {
            sb.append("- 如果上述上下文已足够回答，直接回答；否则先调用工具检索\n");
        }
        sb.append("- 回答末尾列出引用来源清单和下一步学习建议\n");

        return sb.toString();
    }

    /**
     * 从 state 中安全解析 Long 值（兼容 Number / String 两种存储形式）。
     */
    private Long parseLongFromState(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ignored) {
                // ignore
            }
        }
        return null;
    }

    /**
     * Builds trusted context that is available to tools but excluded from the model-generated schema.
     */
    private Map<String, Object> buildToolContext(OverAllState state) {
        Map<String, Object> context = new HashMap<>();
        putIfPresent(context, KnowledgeTools.CONTEXT_USER_ID,
                state.value(RouteKeys.USER_ID));
        putIfPresent(context, KnowledgeTools.CONTEXT_MATERIAL_ID,
                state.value(RouteKeys.MATERIAL_ID));
        putIfPresent(context, KnowledgeTools.CONTEXT_FOLDER_ID,
                state.value(RouteKeys.FOLDER_ID));
        putIfPresent(context, KnowledgeTools.CONTEXT_USER_PROFILE,
                state.value(RouteKeys.COMPRESSED_PROFILE, ""));
        putIfPresent(context, KnowledgeTools.CONTEXT_CONVERSATION_ID,
                state.value(RouteKeys.CONVERSATION_ID, ""));
        putIfPresent(context, KnowledgeTools.CONTEXT_TEMPORARY_MATERIAL_TOKEN,
                state.value(RouteKeys.TEMPORARY_MATERIAL_TOKEN, ""));
        return context;
    }

    private void putIfPresent(Map<String, Object> context, String key, Object value) {
        if (value == null || (value instanceof String text && text.isBlank())) {
            return;
        }
        context.put(key, value);
    }

    private void appendHistoryChunks(StringBuilder sb, Object value) {
        if (!(value instanceof List<?> chunks) || chunks.isEmpty()) {
            return;
        }
        sb.append("**历史对话片段**：\n");
        int index = 1;
        for (Object chunk : chunks) {
            if (chunk instanceof Document doc) {
                sb.append("[来源:").append(index).append("] ");
                sb.append(truncate(doc.getText(), 300)).append("\n\n");
                index++;
            }
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "...";
    }
}
