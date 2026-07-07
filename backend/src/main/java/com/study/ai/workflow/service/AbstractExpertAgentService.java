package com.study.ai.workflow.service;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.study.ai.agent.config.AgentClientFactory;
import com.study.ai.agent.tool.KnowledgeTools;
import com.study.ai.workflow.graph.RouteKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import reactor.core.publisher.Flux;

import java.util.List;

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
    private final String agentId;
    private final String expertId;

    protected AbstractExpertAgentService(AgentClientFactory agentClientFactory,
                                         KnowledgeTools knowledgeTools,
                                         String agentId,
                                         String expertId) {
        this.agentClientFactory = agentClientFactory;
        this.knowledgeTools = knowledgeTools;
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
        if (!materialText.isBlank() || !materialSummary.isBlank()) {
            sb.append("## 3. 资料向量检索结果\n");
            if (!materialSummary.isBlank()) {
                sb.append("**资料描述**：").append(truncate(materialSummary, 800)).append("\n\n");
            }
            if (!materialText.isBlank()) {
                sb.append("**资料摘录**：\n").append(truncate(materialText, MAX_MATERIAL_CHARS)).append("\n\n");
            }
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
        sb.append("- 领域知识库检索结果需通过工具获取：searchDomainKnowledge(domain, query)\n");
        sb.append("- 用户资料补充检索：searchPersonalMaterial(userId, query)\n");
        sb.append("- 历史对话补充检索：searchConversationHistory(userId, query)\n");
        sb.append("- 如果上述上下文已足够回答，直接回答；否则先调用工具检索\n");
        sb.append("- 回答末尾列出引用来源清单和下一步学习建议\n");

        return sb.toString();
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
