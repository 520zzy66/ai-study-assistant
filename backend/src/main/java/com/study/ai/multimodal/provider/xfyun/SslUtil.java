package com.study.ai.multimodal.provider.xfyun;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * SSL 工具类 —— 用于开发环境跳过证书验证。
 *
 * <p>⚠️ 仅限开发/测试环境使用，生产环境必须使用正规证书验证。
 */
public final class SslUtil {

    private SslUtil() {
    }

    /**
     * 获取信任所有证书的 SSLContext。
     *
     * @return 不验证证书的 SSLContext
     */
    public static SSLContext createTrustAllContext() {
        try {
            TrustManager[] trustAll = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }

                        @Override
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                            // 信任所有
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                            // 信任所有
                        }
                    }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAll, new java.security.SecureRandom());
            return sslContext;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create trust-all SSLContext", e);
        }
    }
}
