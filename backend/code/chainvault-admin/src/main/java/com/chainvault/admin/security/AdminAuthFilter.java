package com.chainvault.admin.security;

import com.chainvault.admin.config.AdminJwtProperties;
import com.chainvault.common.constants.AuthConstants;
import com.chainvault.common.exception.BusinessException;
import com.chainvault.common.result.ApiResult;
import com.chainvault.core.service.AdminAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 运营后台 JWT 鉴权过滤器
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
@RequiredArgsConstructor
public class AdminAuthFilter extends OncePerRequestFilter {

    private final AdminJwtProperties properties;
    private final JwtTokenProvider jwtTokenProvider;
    private final AdminAuthService adminAuthService;
    private final ObjectMapper objectMapper;

    /**
     * 校验 JWT 并写入用户上下文
     *
     * @param request     请求
     * @param response    响应
     * @param filterChain 过滤器链
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 1. 跳过非 Admin API 或免鉴权路径
        String uri = request.getRequestURI();
        if (!properties.isAuthEnabled() || !uri.startsWith("/admin/api/") || isPublicPath(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 2. 提取 Bearer Token
            String token = resolveToken(request);
            if (!StringUtils.hasText(token)) {
                writeError(response, 401, "未登录或令牌缺失");
                return;
            }

            // 3. 解析并校验黑名单
            Claims claims = jwtTokenProvider.parseToken(token);
            String jti = claims.getId();
            if (adminAuthService.isTokenBlacklisted(jti)) {
                writeError(response, 401, "登录已失效，请重新登录");
                return;
            }

            // 4. 写入请求上下文
            Long userId = claims.get(AuthConstants.CLAIM_USER_ID, Long.class);
            String username = claims.get(AuthConstants.CLAIM_USERNAME, String.class);
            request.setAttribute(AuthConstants.ATTR_ADMIN_USER_ID, userId);
            request.setAttribute(AuthConstants.ATTR_ADMIN_USERNAME, username);

            filterChain.doFilter(request, response);
        } catch (BusinessException e) {
            writeError(response, e.getCode(), e.getMessage());
        }
    }

    // 从 Authorization 头解析 Bearer Token
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(AuthConstants.HEADER_AUTHORIZATION);
        if (!StringUtils.hasText(header) || !header.startsWith(AuthConstants.BEARER_PREFIX)) {
            return null;
        }
        return header.substring(AuthConstants.BEARER_PREFIX.length()).trim();
    }

    // 判断是否免鉴权路径
    private boolean isPublicPath(String uri) {
        for (String prefix : properties.getPublicPaths()) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    // 输出 JSON 错误
    private void writeError(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResult.fail(code, message)));
    }
}
