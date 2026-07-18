package com.study.ai.multimodal.model;

/**
 * Provider 错误类型枚举。
 *
 * <p>仅暴露业务可理解的错误分类，不包含讯飞原始错误码或鉴权细节。
 */
public enum ProviderErrorType {
    /** 鉴权失败 */
    AUTH_FAILED,
    /** 配置缺失（密钥未配置 / 能力未启用） */
    CONFIG_MISSING,
    /** 触发限流 */
    RATE_LIMITED,
    /** 内容安全审核未通过 */
    CONTENT_REJECTED,
    /** 文本超长 */
    TEXT_TOO_LONG,
    /** 调用超时 */
    TIMEOUT,
    /** 远端服务错误 */
    REMOTE_ERROR,
    /** 响应格式非法 */
    INVALID_RESPONSE,
    /** 文件缺失（下载时磁盘文件不存在） */
    FILE_MISSING,
    /** 未知错误 */
    UNKNOWN
}
