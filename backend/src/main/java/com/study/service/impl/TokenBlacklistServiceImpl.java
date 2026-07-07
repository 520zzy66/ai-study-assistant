package com.study.service.impl;

import com.study.service.TokenBlacklistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Token 黑名单服务实现（Redis 版）
 * 登出时将 Token JTI 加入黑名单，TTL 等于 Token 剩余有效期
 * Redis 不可用时降级为不拦截（仅日志告警）
 */
@Slf4j
@Service
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    /** Redis key 前缀 */
    private static final String KEY_PREFIX = "token:blacklist:";

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Override
    public void blacklist(String jti, long expirationMs) {
        if (redisTemplate == null) {
            log.warn("Redis 不可用，Token 黑名单写入跳过，jti={}", jti);
            return;
        }

        try {
            String key = KEY_PREFIX + jti;
            redisTemplate.opsForValue().set(key, "1", Duration.ofMillis(expirationMs));
            log.info("Token 已加入黑名单，jti={}, TTL={}ms", jti, expirationMs);
        } catch (Exception e) {
            log.warn("Redis 操作失败，Token 黑名单写入跳过: {}", e.getMessage());
        }
    }

    @Override
    public boolean isBlacklisted(String jti) {
        if (redisTemplate == null) {
            // Redis 不可用时降级放行，依赖 JWT 自身过期机制
            return false;
        }

        try {
            String key = KEY_PREFIX + jti;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("Redis 操作失败，降级放行 Token 校验: {}", e.getMessage());
            return false;
        }
    }
}
