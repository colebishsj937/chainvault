package com.chainvault.chainnode.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 链节点 RPC HTTP 客户端工厂（支持 HTTP 代理）
 *
 * @author chainvault
 * @date 2026-06-07
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RpcHttpClientFactory {

    private static final int CONNECT_TIMEOUT_SECONDS = 10;
    private static final int READ_TIMEOUT_SECONDS = 30;
    private static final int WRITE_TIMEOUT_SECONDS = 30;

    private final ChainNodeProperties chainNodeProperties;

    /**
     * 启动时输出代理配置状态
     */
    @PostConstruct
    public void logProxyConfig() {
        ChainNodeProperties.RpcProxy proxy = chainNodeProperties.getRpcProxy();
        if (isProxyEnabled(proxy)) {
            log.info("链节点 RPC 已启用 HTTP 代理 {}:{}", proxy.getHost().trim(), proxy.getPort());
        }
    }

    /**
     * 创建 Web3j 使用的 OkHttp 客户端
     *
     * @return OkHttp 客户端
     */
    public OkHttpClient createOkHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        applyOkHttpProxy(builder);
        return builder.build();
    }

    /**
     * 创建 Java HttpClient（TronGrid / Bitcoin Core RPC）
     *
     * @return HttpClient 实例
     */
    public HttpClient createJavaHttpClient() {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS));
        applyJavaHttpProxy(builder);
        return builder.build();
    }

    // 为 OkHttp 配置 HTTP 代理
    private void applyOkHttpProxy(OkHttpClient.Builder builder) {
        ChainNodeProperties.RpcProxy proxy = chainNodeProperties.getRpcProxy();
        if (!isProxyEnabled(proxy)) {
            return;
        }

        InetSocketAddress address = new InetSocketAddress(proxy.getHost().trim(), proxy.getPort());
        builder.proxy(new Proxy(Proxy.Type.HTTP, address));

        if (StringUtils.hasText(proxy.getUsername())) {
            String password = proxy.getPassword() != null ? proxy.getPassword() : "";
            builder.proxyAuthenticator((route, response) -> {
                String credential = Credentials.basic(proxy.getUsername().trim(), password);
                return response.request().newBuilder()
                        .header("Proxy-Authorization", credential)
                        .build();
            });
        }
    }

    // 为 Java HttpClient 配置 HTTP 代理
    private void applyJavaHttpProxy(HttpClient.Builder builder) {
        ChainNodeProperties.RpcProxy proxy = chainNodeProperties.getRpcProxy();
        if (!isProxyEnabled(proxy)) {
            return;
        }

        InetSocketAddress address = new InetSocketAddress(proxy.getHost().trim(), proxy.getPort());
        builder.proxy(ProxySelector.of(address));

        if (StringUtils.hasText(proxy.getUsername())) {
            String username = proxy.getUsername().trim();
            char[] password = proxy.getPassword() != null
                    ? proxy.getPassword().toCharArray()
                    : new char[0];
            builder.authenticator(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    if (getRequestorType() == RequestorType.PROXY) {
                        return new PasswordAuthentication(username, password);
                    }
                    return null;
                }
            });
        }
    }

    // 判断代理是否启用且配置完整
    private boolean isProxyEnabled(ChainNodeProperties.RpcProxy proxy) {
        if (proxy == null || !proxy.isEnabled()) {
            return false;
        }
        if (!StringUtils.hasText(proxy.getHost()) || proxy.getPort() <= 0) {
            log.warn("RPC 代理已启用但 host/port 无效，将忽略代理配置");
            return false;
        }
        return true;
    }
}
