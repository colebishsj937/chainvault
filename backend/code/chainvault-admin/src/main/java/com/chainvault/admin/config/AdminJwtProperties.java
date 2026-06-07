package com.chainvault.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 运营后台 JWT 与鉴权配置
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
@Component
@ConfigurationProperties(prefix = "chainvault.admin")
public class AdminJwtProperties {

    /** 是否启用 JWT 鉴权 */
    private boolean authEnabled = true;

    /** JWT 配置 */
    private Jwt jwt = new Jwt();

    /** 免鉴权路径前缀 */
    private List<String> publicPaths = new ArrayList<>(List.of("/admin/api/v1/auth/login"));

    /**
     * JWT 参数
     */
    @Data
    public static class Jwt {

        /** 签名密钥（生产环境务必通过环境变量注入，长度建议 ≥ 32） */
        private String secret = "chainvault-dev-jwt-secret-change-in-production";

        /** 令牌有效期（小时） */
        private int expirationHours = 24;
    }
}
