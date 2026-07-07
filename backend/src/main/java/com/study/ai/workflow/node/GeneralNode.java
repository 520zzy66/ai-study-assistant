package com.study.ai.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.ai.agent.config.AgentClientFactory;
import com.study.ai.history.ConversationHistoryVectorizer;
import com.study.ai.memory.BoundedChatMemory;
import com.study.ai.profile.UserProfileCompressor;
import com.study.ai.workflow.graph.RouteKeys;
import com.study.entity.AiChatHistory;
import com.study.entity.UserMemoryEntry;
import com.study.mapper.AiChatHistoryMapper;
import com.study.service.UserMemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 通用路由节点 — 三级路由决策中心。
 *
 * <p>职责：
 * <ol>
 *   <li>会话向量化（本地 Ollama Embedding）</li>
 *   <li>上下文注入（用户画像、长期记忆、历史对话向量、资料分析）</li>
 *   <li>三级路由决策：
 *       Level 0 — 关键词/问候直接回答（本地规则，无 LLM）
 *       Level 1 — 简单常识问题（LLM 直接回答）
 *       Level 2 — 专家分析（路由到 ExpertAgentService）</li>
 *   <li>简单问题回答（Level 0/1 生成 localAnswer）</li>
 * </ol>
 */
@Slf4j
@Component
public class GeneralNode implements NodeAction {

    /** 问候/告别关键词 — Level 0 直接回答 */
    private static final Map<String, String> GREETING_RESPONSES = Map.ofEntries(
            Map.entry("你好", "你好！我是你的 AI 学习助手，可以帮你解答考公、考研相关的问题，或者分析你的学习资料。有什么可以帮你的？"),
            Map.entry("hello", "Hello! 我是你的 AI 学习助手，有什么可以帮你的吗？"),
            Map.entry("嗨", "嗨！准备好学习了吗？有什么问题尽管问~"),
            Map.entry("谢谢", "不客气！有问题随时问我哦~"),
            Map.entry("thanks", "You're welcome! 有问题随时问我~"),
            Map.entry("再见", "再见！祝你学习进步，考试顺利！"),
            Map.entry("bye", "Bye! 加油！")
    );

    /** 简单问题模式 — Level 1 用 LLM 直接回答 */
    private static final List<Pattern> SIMPLE_PATTERNS = List.of(
            Pattern.compile("^(什么是|介绍一下|解释一下|定义)\\s*\\S{1,15}$"),
            Pattern.compile("^(\\S{1,10}(是什么|是啥|啥意思|什么意思|的概念))$"),
            Pattern.compile("^(翻译|translate)\\s*", Pattern.CASE_INSENSITIVE)
    );

    /** 领域关键词 — Layer 0 快速路由 */
    private static final Map<String, List<String>> DOMAIN_KEYWORDS = Map.of(
            "CIVIL", List.of("申论", "行测", "国考", "省考", "面试", "时政", "公务", "公考", "选调", "公务员"),
            "GRADUATE", List.of("考研", "高数", "概率论", "线性代数", "英一", "英二", "复试", "数学一", "数学二", "数学三", "研究生")
    );

    private static final String GENERAL_EXPERT = "GENERAL_EXPERT";
    private static final String CIVIL_EXPERT = "CIVIL_EXPERT";
    private static final String GRADUATE_EXPERT = "GRADUATE_EXPERT";

    private final AgentClientFactory agentClientFactory;
    private final UserProfileCompressor profileCompressor;
    private final ConversationHistoryVectorizer historyVectorizer;
    private final UserMemoryService userMemoryService;
    private final BoundedChatMemory boundedChatMemory;
    private final AiChatHistoryMapper chatHistoryMapper;

    @Autowired(required = false)
    private VectorStore vectorStore;

    public GeneralNode(AgentClientFactory agentClientFactory,
                       UserProfileCompressor profileCompressor,
                       ConversationHistoryVectorizer historyVectorizer,
                       UserMemoryService userMemoryService,
                       BoundedChatMemory boundedChatMemory,
                       AiChatHistoryMapper chatHistoryMapper) {
        this.agentClientFactory = agentClientFactory;
        this.profileCompressor = profileCompressor;
        this.historyVectorizer = historyVectorizer;
        this.userMemoryService = userMemoryService;
        this.boundedChatMemory = boundedChatMemory;
        this.chatHistoryMapper = chatHistoryMapper;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String query = state.value(RouteKeys.QUERY, "").trim();
        Long userId = parseLong(state.value(RouteKeys.USER_ID));
        String conversationId = state.value(RouteKeys.CONVERSATION_ID, "");
        String materialSummary = state.value(RouteKeys.MATERIAL_SUMMARY, "");

        if (query.isBlank()) {
            return buildFallbackState(query, "用户问题为空");
        }

        log.info("[GeneralNode] 开始路由：query={}, userId={}, hasMaterial={}",
                truncate(query, 80), userId, !materialSummary.isBlank());

        // ========== Step 1: 会话向量化（本地 Ollama） ==========
        vectorizeLastConversation(userId, conversationId);

        // ========== Step 2: 上下文注入（必须） ==========
        String compressedProfile = profileCompressor.getCachedProfile(userId);
        String memoryContext = getMemoryContext(userId, query);
        List<Document> historyChunks = searchHistoryChunks(userId, query);
        String recentContext = getRecentContext(conversationId);

        // ========== Step 3: 路由决策 + 回答生成 ==========
        String localAnswer = "";
        int routeLevel;
        String routedExpert;
        String routeDomain;
        String intent;
        double confidence;

        // Level 0: 关键词直接回答
        localAnswer = tryKeywordMatch(query);
        if (localAnswer != null) {
            routeLevel = 0;
            routedExpert = GENERAL_EXPERT;
            routeDomain = "GENERAL";
            intent = "问候/关键词直接回答";
            confidence = 1.0;
        }
        // Level 1: 简单问题（LLM 生成回答）
        else if (isSimpleQuestion(query) && materialSummary.isBlank()) {
            log.debug("[GeneralNode] Level 1 匹配：isSimpleQuestion=true, materialSummary.isBlank()={}", materialSummary.isBlank());
            localAnswer = generateSimpleAnswer(query, compressedProfile, memoryContext);
            log.debug("[GeneralNode] generateSimpleAnswer 返回：{}", localAnswer != null ? localAnswer.substring(0, Math.min(100, localAnswer.length())) : "null");
            if (localAnswer != null) {
                routeLevel = 1;
                routedExpert = GENERAL_EXPERT;
                routeDomain = "GENERAL";
                intent = "简单常识问题";
                confidence = 0.85;
            } else {
                // LLM 生成失败，回退到专家路由
                RouteDecision decision = routeToExpert(query, materialSummary, compressedProfile);
                routeLevel = 2;
                routedExpert = decision.expertId();
                routeDomain = decision.domain();
                intent = decision.intent();
                confidence = decision.confidence();
                localAnswer = "";
            }
        }
        // Level 2: 专家路由
        else {
            RouteDecision decision = routeToExpert(query, materialSummary, compressedProfile);
            routeLevel = 2;
            routedExpert = decision.expertId();
            routeDomain = decision.domain();
            intent = decision.intent();
            confidence = decision.confidence();
            localAnswer = "";
        }

        // ========== 写入 state ==========
        Map<String, Object> output = new LinkedHashMap<>();
        output.put(RouteKeys.ROUTE_LEVEL, routeLevel);
        output.put(RouteKeys.ROUTED_EXPERT, routedExpert);
        output.put(RouteKeys.ROUTE_DOMAIN, routeDomain);
        output.put(RouteKeys.ROUTE_INTENT, intent);
        output.put(RouteKeys.ROUTE_CONFIDENCE, confidence);
        output.put(RouteKeys.ROUTING_SUMMARY, buildRoutingSummary(query, routeDomain, intent));
        output.put(RouteKeys.USER_QUERY, query);
        output.put(RouteKeys.COMPRESSED_PROFILE, compressedProfile);
        output.put(RouteKeys.MEMORY_CONTEXT, memoryContext);
        output.put(RouteKeys.HISTORY_CHUNKS, historyChunks);
        output.put(RouteKeys.RECENT_CONTEXT, recentContext);
        output.put(RouteKeys.LOCAL_ANSWER, localAnswer);

        log.info("[GeneralNode] 路由完成：level={}, expert={}, domain={}",
                routeLevel, routedExpert, routeDomain);

        return output;
    }

    // ==================== 会话向量化 ====================

    private void vectorizeLastConversation(Long userId, String conversationId) {
        if (userId == null || userId <= 0) return;
        try {
            AiChatHistory lastChat = chatHistoryMapper.selectOne(
                    new LambdaQueryWrapper<AiChatHistory>()
                            .eq(AiChatHistory::getUserId, userId)
                            .eq(AiChatHistory::getConversationId, conversationId)
                            .orderByDesc(AiChatHistory::getCreateTime)
                            .last("LIMIT 1")
            );
            if (lastChat != null) {
                historyVectorizer.vectorizeSync(List.of(lastChat));
                log.debug("[GeneralNode] 会话向量化完成：chatId={}", lastChat.getId());
            }
        } catch (Exception e) {
            log.warn("[GeneralNode] 会话向量化失败：{}", e.getMessage());
        }
    }

    // ==================== 上下文注入 ====================

    private String getMemoryContext(Long userId, String query) {
        if (userId == null || userId <= 0) return "";
        try {
            List<UserMemoryEntry> memories = userMemoryService.searchMemories(userId, query, 5);
            if (memories == null || memories.isEmpty()) return "";
            return memories.stream()
                    .map(m -> "[" + m.getCategory() + "] " + m.getContent())
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.warn("[GeneralNode] 记忆检索失败：{}", e.getMessage());
            return "";
        }
    }

    private List<Document> searchHistoryChunks(Long userId, String query) {
        if (userId == null || userId <= 0 || vectorStore == null) return List.of();
        try {
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(3)
                    .similarityThreshold(0.2)
                    .filterExpression("source == 'conversation_history' AND user_id == " + userId)
                    .build();
            List<Document> docs = vectorStore.similaritySearch(request);
            return docs != null ? docs : List.of();
        } catch (Exception e) {
            log.debug("[GeneralNode] 历史对话检索失败：{}", e.getMessage());
            return List.of();
        }
    }

    private String getRecentContext(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) return "";
        try {
            List<Message> messages = boundedChatMemory.get(conversationId, 10);
            if (messages == null || messages.isEmpty()) return "";
            return messages.stream()
                    .map(m -> m.getMessageType() + ": " + truncate(m.getText(), 100))
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "";
        }
    }

    // ==================== Level 0: 关键词匹配 ====================

    private String tryKeywordMatch(String query) {
        String lowerQuery = query.toLowerCase().trim();
        for (Map.Entry<String, String> entry : GREETING_RESPONSES.entrySet()) {
            if (lowerQuery.equals(entry.getKey().toLowerCase())
                    || lowerQuery.startsWith(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }
        return null;
    }

    // ==================== Level 1: 简单问题判断 ====================

    private boolean isSimpleQuestion(String query) {
        if (query.length() > 50) return false;
        return SIMPLE_PATTERNS.stream().anyMatch(p -> p.matcher(query).find());
    }

    private String generateSimpleAnswer(String query, String compressedProfile, String memoryContext) {
        try {
            ChatClient client = agentClientFactory.getOrCreate("general");
            String prompt = String.format("""
                    ## 用户问题
                    %s

                    ## 用户画像
                    %s

                    ## 记忆上下文
                    %s

                    这是一个简单问题（问候、常识定义等），请用 1-3 句话直接回答。
                    如果是问候语，友好回应即可。
                    如果是知识定义类问题，简明扼要解释核心概念。
                    不要过度展开，不要编造信息。
                    """,
                    query,
                    compressedProfile != null && !compressedProfile.isBlank() ? compressedProfile : "（无画像）",
                    memoryContext != null && !memoryContext.isBlank() ? memoryContext : "（无记忆）"
            );
            // 覆盖 system prompt（general agent 默认是路由分析器，会输出 JSON）
            return client.prompt()
                    .system("你是一个友好的AI学习助手。请直接回答用户的问题，用简洁的中文回复。不要输出JSON格式，不要进行路由分析。")
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("[GeneralNode] 简单问题回答失败：{}", e.getMessage());
            return null;
        }
    }

    // ==================== Level 2: 专家路由 ====================

    private RouteDecision routeToExpert(String query, String materialSummary, String compressedProfile) {
        // Layer 0: 关键词快速路由
        String fastDomain = detectDomainByKeywords(query);
        if (fastDomain != null) {
            String expertId = switch (fastDomain) {
                case "CIVIL" -> CIVIL_EXPERT;
                case "GRADUATE" -> GRADUATE_EXPERT;
                default -> GENERAL_EXPERT;
            };
            return new RouteDecision(fastDomain, expertId, 0.90,
                    "用户问题包含" + fastDomain + "领域关键词");
        }

        // Layer 1-3: LLM 路由
        return analyzeWithLlm(query, compressedProfile, materialSummary);
    }

    private String detectDomainByKeywords(String query) {
        String lowerQuery = query.toLowerCase();
        for (Map.Entry<String, List<String>> entry : DOMAIN_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lowerQuery.contains(keyword.toLowerCase())) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    private RouteDecision analyzeWithLlm(String query, String compressedProfile, String materialSummary) {
        RouteDecision fallback = new RouteDecision("GENERAL", GENERAL_EXPERT, 0.5,
                "无法确定领域，回退到通用专家");

        try {
            ChatClient client = agentClientFactory.getOrCreate("general");

            // 对齐 general-system.txt 定义的输入格式
            String prompt = String.format("""
                    ## 用户原始问题
                    %s

                    ## 资料详细描述
                    %s

                    ## 用户学习画像
                    %s

                    请综合以上信息，严格返回 JSON（不要 markdown 代码块）：
                    {"domain":"CIVIL|GRADUATE|GENERAL","confidence":0.0-1.0,"intent":"意图描述","reasoning":"判断依据"}
                    """,
                    truncate(query, 500),
                    materialSummary != null && !materialSummary.isBlank()
                            ? truncate(materialSummary, 300) : "（无资料）",
                    compressedProfile != null && !compressedProfile.isBlank()
                            ? truncate(compressedProfile, 200) : "（无画像）"
            );

            String response = client.prompt().user(prompt).call().content();
            if (response != null && !response.isBlank()) {
                return parseRouteResponse(response, fallback);
            }
        } catch (Exception e) {
            log.warn("[GeneralNode] LLM 路由分析失败，回退到通用专家：{}", e.getMessage());
        }

        return fallback;
    }

    private RouteDecision parseRouteResponse(String response, RouteDecision fallback) {
        try {
            String json = response.trim();
            if (json.contains("```")) {
                json = json.replaceAll("```json?\\s*", "").replaceAll("```\\s*$", "");
            }
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start < 0 || end <= start) return fallback;
            json = json.substring(start, end + 1);

            String domain = extractJsonField(json, "domain");
            String intent = extractJsonField(json, "intent");
            String confidenceStr = extractJsonField(json, "confidence");

            if (domain == null) return fallback;

            domain = domain.toUpperCase();
            if (!domain.equals("CIVIL") && !domain.equals("GRADUATE")) {
                domain = "GENERAL";
            }

            String expertId = switch (domain) {
                case "CIVIL" -> CIVIL_EXPERT;
                case "GRADUATE" -> GRADUATE_EXPERT;
                default -> GENERAL_EXPERT;
            };

            double confidence = 0.5;
            if (confidenceStr != null) {
                try {
                    confidence = Double.parseDouble(confidenceStr);
                    confidence = Math.max(0.0, Math.min(1.0, confidence));
                } catch (NumberFormatException ignored) {
                }
            }

            return new RouteDecision(domain, expertId, confidence,
                    intent != null ? intent : "通用问题分析");
        } catch (Exception e) {
            return fallback;
        }
    }

    private String extractJsonField(String json, String field) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "\"" + field + "\"\\s*:\\s*\"?([^,\"}]+)\"?");
        java.util.regex.Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    // ==================== 辅助方法 ====================

    private Map<String, Object> buildFallbackState(String query, String reason) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put(RouteKeys.ROUTE_LEVEL, 2);
        output.put(RouteKeys.ROUTED_EXPERT, GENERAL_EXPERT);
        output.put(RouteKeys.ROUTE_DOMAIN, "GENERAL");
        output.put(RouteKeys.ROUTE_INTENT, "回退：" + reason);
        output.put(RouteKeys.ROUTE_CONFIDENCE, 0.0);
        output.put(RouteKeys.LOCAL_ANSWER, "");
        output.put(RouteKeys.ROUTING_SUMMARY, "路由异常，回退到通用专家：" + reason);
        // 注入空上下文，避免下游读取时出现 null
        output.put(RouteKeys.COMPRESSED_PROFILE, "");
        output.put(RouteKeys.MEMORY_CONTEXT, "");
        output.put(RouteKeys.HISTORY_CHUNKS, List.of());
        output.put(RouteKeys.RECENT_CONTEXT, "");
        return output;
    }

    private String buildRoutingSummary(String query, String domain, String intent) {
        return String.format("用户问题：%s\n领域：%s\n意图：%s",
                truncate(query, 200), domain, intent);
    }

    private Long parseLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        if (value instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }

    private record RouteDecision(String domain, String expertId, double confidence, String intent) {
    }
}
