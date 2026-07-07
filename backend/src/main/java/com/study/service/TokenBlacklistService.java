package com.study.service;

/**
 * Token 黑名单服务
 * 用于登出后使已签发的 JWT 失效
 */
public interface TokenBlacklistService {

    /**
     * 将 Token 加入黑名单
     *
     * @param jti        JWT ID
     * @param expiration Token 剩余有效时间（毫秒）
     */
    void blacklist(String jti, long expiration);

    /**
     * 检查 Token 是否在黑名单中
     *
     * @param jti JWT ID
     * @return true 表示已失效
     */
    boolean isBlacklisted(String jti);
}
