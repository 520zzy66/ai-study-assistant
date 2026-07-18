package com.study.dto.request;

import lombok.Data;

/**
 * 资产重试请求。
 *
 * <p>重试默认复用原始 {@code promptSummary} 或 {@code metadata.originalPrompt}，
 * 因此请求体无必填字段。后续可扩展为允许覆盖提示词。
 */
@Data
public class RetryResourceAssetRequest {
    // 当前无必填字段；保留类以备后续扩展（如覆盖提示词、切换发音人）
}
