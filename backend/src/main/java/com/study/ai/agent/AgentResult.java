package com.study.ai.agent;

/**
 * Agent 执行结果
 *
 * @author AI Study Assistant
 */
public class AgentResult {

    /** 结果文本 */
    private String content;

    /** 是否成功 */
    private boolean success;

    /** 置信度（0.0-1.0） */
    private double confidence;

    /** Token 消耗 */
    private int tokensUsed;

    /** 耗时（毫秒） */
    private long durationMs;

    /** 错误信息（success=false 时） */
    private String errorMessage;

    public AgentResult() {}

    public static AgentResult success(String content) {
        AgentResult r = new AgentResult();
        r.content = content;
        r.success = true;
        return r;
    }

    public static AgentResult error(String errorMessage) {
        AgentResult r = new AgentResult();
        r.success = false;
        r.errorMessage = errorMessage;
        return r;
    }

    // ===== Getters & Setters =====

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public int getTokensUsed() { return tokensUsed; }
    public void setTokensUsed(int tokensUsed) { this.tokensUsed = tokensUsed; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
