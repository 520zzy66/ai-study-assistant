package com.study.common;

import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * 安全工具类
 * 提供 URL 安全校验、内网 IP 过滤等防护功能
 */
@Slf4j
public final class SecurityUtils {

    private SecurityUtils() {}

    /** 允许的 URL 协议 */
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    /** 内网 IP 前缀（RFC 1918 + 回环地址） */
    private static final String[] PRIVATE_IP_PREFIXES = {
            "10.", "127.", "169.254.", "192.168.", "172.16.", "172.17.",
            "172.18.", "172.19.", "172.20.", "172.21.", "172.22.", "172.23.",
            "172.24.", "172.25.", "172.26.", "172.27.", "172.28.", "172.29.",
            "172.30.", "172.31."
    };

    /**
     * 校验 URL 是否安全（防 SSRF）
     * 检查：协议白名单 + 内网 IP 过滤 + DNS 重绑定防护
     *
     * @param url 待校验的 URL
     * @throws BusinessException 如果 URL 不安全
     */
    public static void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new BusinessException(400, "URL 不能为空");
        }

        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (Exception e) {
            throw new BusinessException(400, "URL 格式不正确");
        }

        // 1. 协议白名单
        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            throw new BusinessException(400, "仅支持 http/https 协议");
        }

        // 2. 主机名校验
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new BusinessException(400, "URL 缺少主机名");
        }

        // 3. 内网 IP 过滤（直接 IP 地址）
        if (isPrivateIp(host)) {
            log.warn("拒绝访问内网地址: host={}", host);
            throw new BusinessException(400, "不允许访问内网地址");
        }

        // 4. DNS 重绑定防护：解析域名后的 IP 也要检查
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress addr : addresses) {
                if (isPrivateIp(addr.getHostAddress())) {
                    log.warn("DNS 重绑定检测: host={} 解析到内网 IP {}", host, addr.getHostAddress());
                    throw new BusinessException(400, "目标地址解析到内网，已拒绝");
                }
            }
        } catch (UnknownHostException e) {
            throw new BusinessException(400, "无法解析主机名: " + host);
        }
    }

    /**
     * 判断是否为内网 IP
     */
    private static boolean isPrivateIp(String ip) {
        if (ip == null || ip.isBlank()) return false;
        for (String prefix : PRIVATE_IP_PREFIXES) {
            if (ip.startsWith(prefix)) return true;
        }
        // IPv6 回环
        if ("::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) return true;
        return false;
    }

    /**
     * 清理文件名中的危险字符（防路径穿越）
     *
     * @param filename 原始文件名
     * @return 安全的文件名
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "unnamed";
        }
        // 取最后一段路径（防路径穿越）
        String safe = filename.replace("\\", "/");
        int lastSlash = safe.lastIndexOf('/');
        if (lastSlash >= 0) {
            safe = safe.substring(lastSlash + 1);
        }
        // 替换危险字符
        safe = safe.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1f]", "_");
        // 限制长度
        if (safe.length() > 200) {
            safe = safe.substring(0, 200);
        }
        return safe.isEmpty() ? "unnamed" : safe;
    }
}
