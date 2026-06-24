package com.study.service.impl;

import com.study.common.Constants;
import com.study.service.LoginAttemptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 登录尝试服务实现（Redis 版）
 * 使用 Redis 记录登录失败次数，连续 5 次失败锁定 15 分钟
 * Redis 不可用时降级为不限制
 */
@Slf4j
@Service
public class LoginAttemptServiceImpl implements LoginAttemptService {

    /** 锁定时长 */
    private static final Duration LOCK_DURATION = Duration.ofMinutes(Constants.LOGIN_LOCK_MINUTES);

    /** Redis key 前缀 */
    private static final String KEY_PREFIX = "login:attempt:";

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Override
    public void loginSucceeded(String username) {
        if (redisTemplate == null) return;

        try {
            String key = KEY_PREFIX + username;
            redisTemplate.delete(key);
            log.debug("用户 {} 登录成功，清除失败记录", username);
        } catch (Exception e) {
            log.warn("Redis 操作失败，跳过清除登录记录: {}", e.getMessage());
        }
    }

    @Override
    public boolean checkAndIncrement(String username) {
        if (redisTemplate == null) return true;

        try {
            String key = KEY_PREFIX + username;
            Long attempts = redisTemplate.opsForValue().increment(key);
            if (attempts == null) return true;

            // 首次失败时设置 TTL
            if (attempts == 1) {
                redisTemplate.expire(key, LOCK_DURATION);
            }

            // 达到锁定阈值
            if (attempts >= Constants.LOGIN_MAX_ATTEMPTS) {
                redisTemplate.expire(key, LOCK_DURATION);
                log.warn("用户 {} 连续失败 {} 次，已锁定 {} 分钟", username, attempts,
                        LOCK_DURATION.toMinutes());
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("Redis 操作失败，降级放行登录: {}", e.getMessage());
            return true;
        }
    }
}
