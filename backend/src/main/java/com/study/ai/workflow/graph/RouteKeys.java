package com.study.ai.workflow.graph;

/**
 * State keys used in the workflow graph's {@link com.alibaba.cloud.ai.graph.OverAllState}.
 *
 * <p>All nodes read from and write to the shared state using these keys.
 */
public final class RouteKeys {

    private RouteKeys() {
    }

    // ==================== Input keys (set by WorkflowGraphService.buildInitialState) ====================

    /** User question text. */
    public static final String QUERY = "query";

    /** Current user ID. */
    public static final String USER_ID = "userId";

    /** Optional material full text prepared by upstream modules. */
    public static final String MATERIAL_TEXT = "materialText";

    /** Optional material summary produced by multimodal node. */
    public static final String MATERIAL_SUMMARY = "materialSummary";

    /** Optional material ID. */
    public static final String MATERIAL_ID = "materialId";

    /** Conversation ID for multi-turn context. */
    public static final String CONVERSATION_ID = "conversationId";

    /** Frontend-provided conversation turns. */
    public static final String FRONTEND_HISTORY = "frontendHistory";

    // ==================== Routing output keys (set by GeneralNode) ====================

    /** Route level: 0 local answer, 1 simple question, 2 expert analysis. */
    public static final String ROUTE_LEVEL = "routeLevel";

    /** Routed expert ID: CIVIL_EXPERT, GRADUATE_EXPERT or GENERAL_EXPERT. */
    public static final String ROUTED_EXPERT = "routedExpert";

    /** Route domain: CIVIL, GRADUATE or GENERAL. */
    public static final String ROUTE_DOMAIN = "routeDomain";

    /** Route intent description. */
    public static final String ROUTE_INTENT = "routeIntent";

    /** Route confidence 0.0-1.0. */
    public static final String ROUTE_CONFIDENCE = "routeConfidence";

    /** Local answer for Level 0/1 (empty string when Level 2). */
    public static final String LOCAL_ANSWER = "localAnswer";

    /** Routing summary for downstream expert reference. */
    public static final String ROUTING_SUMMARY = "routingSummary";

    /** User original query (echoed by GeneralNode). */
    public static final String USER_QUERY = "userQuery";

    // ==================== Context keys (injected by GeneralNode) ====================

    /** Compressed user profile text. */
    public static final String COMPRESSED_PROFILE = "compressedProfile";

    /** User memory context (formatted from UserMemoryService). */
    public static final String MEMORY_CONTEXT = "memoryContext";

    /** Relevant conversation history chunks (from VectorStore). */
    public static final String HISTORY_CHUNKS = "historyChatChunks";

    /** Recent conversation context (from BoundedChatMemory). */
    public static final String RECENT_CONTEXT = "recentContext";

    // ==================== Final output keys ====================

    /** Final answer in Markdown. */
    public static final String ANSWER = "answer";

    /** Whether fallback text was returned. */
    public static final String FALLBACK = "fallback";

    /** Error message when fallback is true. */
    public static final String ERROR_MESSAGE = "errorMessage";
}
