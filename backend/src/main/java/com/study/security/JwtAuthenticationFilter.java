package com.study.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.common.Result;
import com.study.common.UserContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * JWT 认证过滤器
 * 从 Authorization 请求头提取 token 并设置用户上下文
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String TOKEN_PREFIX = "Bearer ";

    /** 精确匹配白名单 */
    private static final Set<String> WHITE_LIST_EXACT = Set.of(
            "/auth/register",
            "/auth/login",
            "/doc.html",
            "/actuator/health"
    );

    /** 前缀匹配白名单（仅允许匹配自身及其子路径，如 /swagger-ui/index.html） */
    private static final Set<String> WHITE_LIST_PREFIX = Set.of(
            "/swagger-ui",
            "/v3/api-docs"
    );

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 白名单放行
        if (isWhiteListed(request.getServletPath())) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractToken(request);
        if (token == null) {
            writeError(response, 401, "未登录或token已失效");
            return;
        }

        try {
            Claims claims = jwtUtil.parseToken(token);
            Long userId = claims.get("userId", Long.class);
            String username = claims.get("username", String.class);
            UserContext.setCurrentUser(new UserContext.UserInfo(userId, username));

            // Token 刷新：剩余 2 小时内返回新 token
            if (jwtUtil.isNearExpiry(token)) {
                String newToken = jwtUtil.generateToken(userId, username);
                response.setHeader("X-New-Token", newToken);
            }
        } catch (ExpiredJwtException e) {
            log.debug("token已过期", e);
            writeError(response, 401, "token已过期，请重新登录");
            return;
        } catch (Exception e) {
            log.debug("token解析失败", e);
            writeError(response, 401, "无效的token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头提取 token
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(TOKEN_PREFIX)) {
            return header.substring(TOKEN_PREFIX.length());
        }
        return null;
    }

    /**
     * 判断是否在白名单中
     * 精确匹配 + 前缀匹配（仅允许子路径，如 /swagger-ui/index.html）
     * /actuator 仅精确匹配，防止 /actuator/env 等敏感端点被误放行
     */
    private boolean isWhiteListed(String path) {
        // 精确匹配
        if (WHITE_LIST_EXACT.contains(path)) {
            return true;
        }
        // 前缀匹配：自身 或 子路径（/prefix/xxx）
        for (String prefix : WHITE_LIST_PREFIX) {
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 返回 JSON 格式错误响应
     * 使用传入的 HTTP 状态码，不再固定返回 200
     */
    private void writeError(HttpServletResponse response, int httpStatus, String message) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Result<Void> result = Result.error(httpStatus, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
