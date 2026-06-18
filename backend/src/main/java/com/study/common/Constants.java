package com.study.common;

/**
 * 项目常量
 * 集中管理全局常量，避免魔法值散落各处
 */
public final class Constants {

    private Constants() {
        // 工具类，禁止实例化
    }

    // === 分页 ===
    /** 默认页码 */
    public static final int DEFAULT_PAGE = 1;
    /** 默认每页条数 */
    public static final int DEFAULT_SIZE = 10;
    /** 最大每页条数 */
    public static final int MAX_SIZE = 100;

    // === Token ===
    /** Token 刷新阈值（2小时） */
    public static final long TOKEN_REFRESH_THRESHOLD_MS = 2 * 60 * 60 * 1000;

    // === 资料状态 ===
    /** 处理中（文件已上传，等待解析） */
    public static final String STATUS_PROCESSING = "processing";
    /** 解析中（正在提取文本内容） */
    public static final String STATUS_PARSING = "parsing";
    /** 就绪（解析完成，可用于 AI 功能） */
    public static final String STATUS_READY = "ready";
    /** 失败（解析或向量化失败） */
    public static final String STATUS_FAILED = "failed";

    // === 对话类型 ===
    /** 知识总结 */
    public static final String CHAT_TYPE_SUMMARY = "summary";
    /** RAG 文档问答 */
    public static final String CHAT_TYPE_QA = "qa";
    /** AI 自动出题 */
    public static final String CHAT_TYPE_QUIZ = "quiz";
    /** AI 学习计划 */
    public static final String CHAT_TYPE_PLAN = "plan";

    // === 登录 ===
    /** 最大登录失败次数 */
    public static final int LOGIN_MAX_ATTEMPTS = 5;
    /** 登录锁定时长（分钟） */
    public static final long LOGIN_LOCK_MINUTES = 15;
}
