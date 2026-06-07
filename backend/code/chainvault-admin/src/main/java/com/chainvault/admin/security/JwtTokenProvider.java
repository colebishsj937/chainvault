package com.chainvault.admin.security;

import com.chainvault.admin.config.AdminJwtProperties;
import com.chainvault.common.constants.AuthConstants;
import com.chainvault.common.exception.BusinessException;
import com.chainvault.core.domain.entity.AdminUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 令牌生成与解析
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final AdminJwtProperties properties;

    /**
     * 签发访问令牌
     *
     * @param user 登录用户
     * @return JWT 字符串
     */
    public String createToken(AdminUser user) {
        // 1. 计算过期时间
        Instant now = Instant.now();
        long expiresSeconds = properties.getJwt().getExpirationHours() * 3600L;
        Instant expiry = now.plusSeconds(expiresSeconds);
        String jti = UUID.randomUUID().toString().replace("-", "");

        // 2. 构建 JWT
        return Jwts.builder()
                .id(jti)
                .subject(String.valueOf(user.getId()))
                .claim(AuthConstants.CLAIM_USER_ID, user.getId())
                .claim(AuthConstants.CLAIM_USERNAME, user.getUsername())
                .claim(AuthConstants.CLAIM_DISPLAY_NAME, user.getDisplayName())
                .claim(AuthConstants.CLAIM_ROLE, user.getRole())
                .claim(AuthConstants.CLAIM_JTI, jti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey())
                .compact();
    }

    /**
     * 获取令牌有效期（秒）
     *
     * @return 秒数
     */
    public long getExpirationSeconds() {
        return properties.getJwt().getExpirationHours() * 3600L;
    }

    /**
     * 解析并校验令牌
     *
     * @param token JWT 字符串
     * @return Claims
     */
    public Claims parseToken(String token) {
        try {
            // 1. 解析签名与过期时间
            return Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(401, "登录已过期，请重新登录");
        } catch (Exception e) {
            throw new BusinessException(401, "无效的访问令牌");
        }
    }

    /**
     * 计算令牌剩余有效秒数（用于登出黑名单 TTL）
     *
     * @param claims JWT 载荷
     * @return 剩余秒数，最小为 1
     */
    public long remainingSeconds(Claims claims) {
        // 1. 根据过期时间计算剩余 TTL
        Date expiration = claims.getExpiration();
        if (expiration == null) {
            return getExpirationSeconds();
        }
        long remain = expiration.toInstant().getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(remain, 1L);
    }

    // 构建 HMAC 签名密钥
    private SecretKey signingKey() {
        byte[] keyBytes = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
