package com.chainvault.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关配置项
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
@Component
@ConfigurationProperties(prefix = "chainvault.gateway")
public class GatewayProperties {

    /** 是否启用 MD5 签名校验 */
    private boolean signEnabled = true;

    /** 是否启用限流 */
    private boolean rateLimitEnabled = true;

    /** 限流时间窗口（秒） */
    private int rateLimitWindowSeconds = 60;

    /** 限流窗口内最大请求数 */
    private int rateLimitMaxRequests = 200;

    /** 免签名路径前缀 */
    private List<String> publicPaths = new ArrayList<>(List.of(
            "/actuator",
            "/api/v1/system"
    ));
}
