package com.study.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 工具类
 * 负责 token 的生成、解析和验证
 */
@Slf4j
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expiration;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long expiration) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET 环境变量未设置，请配置后重启");
        }
        // 兼容标准 Base64 和 URL-safe Base64（含 - _ 字符）
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64URL.decode(secret);
        } catch (io.jsonwebtoken.io.DecodingException e) {
            keyBytes = Decoders.BASE64.decode(secret);
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expiration = expiration;
    }

    /**
     * 生成 JWT token
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @return JWT token
     */
    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .claim("userId", userId)
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析 JWT token
     *
     * @param token JWT token
     * @return Claims
     * @throws ExpiredJwtException token 过期
     * @throws JwtException        token 无效
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 获取过期时间（毫秒）
     */
    public long getExpiration() {
        return expiration;
    }

    /**
     * 判断 token 是否即将过期（剩余时间 < thresholdMs）
     *
     * @param token       JWT token
     * @param thresholdMs 阈值（毫秒），默认 2 小时 = 7200000
     * @return true 表示即将过期
     */
    public boolean isNearExpiry(String token, long thresholdMs) {
        try {
            Claims claims = parseToken(token);
            long remaining = claims.getExpiration().getTime() - System.currentTimeMillis();
            return remaining > 0 && remaining < thresholdMs;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断 token 是否即将过期（默认阈值 2 小时）
     */
    public boolean isNearExpiry(String token) {
        return isNearExpiry(token, 2 * 60 * 60 * 1000);
    }
}
