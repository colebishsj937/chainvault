package com.chainvault.admin.controller;

import com.chainvault.common.constants.AuthConstants;
import com.chainvault.common.result.ApiResult;
import com.chainvault.core.domain.dto.AdminLoginReq;
import com.chainvault.core.domain.vo.AdminLoginVO;
import com.chainvault.core.domain.vo.AdminUserVO;
import com.chainvault.core.service.AdminAuthService;
import com.chainvault.admin.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运营后台认证 API
 *
 * @author chainvault
 * @date 2026-06-05
 */
@RestController
@RequestMapping("/admin/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AdminAuthService adminAuthService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 用户名密码登录
     *
     * @param req 登录请求
     * @return JWT 与用户信息
     */
    @PostMapping("/login")
    public ApiResult<AdminLoginVO> login(@Valid @RequestBody AdminLoginReq req) {
        return ApiResult.ok(adminAuthService.login(req));
    }

    /**
     * 登出，将当前令牌加入黑名单
     *
     * @param request HTTP 请求
     * @return 成功响应
     */
    @PostMapping("/logout")
    public ApiResult<Void> logout(HttpServletRequest request) {
        // 1. 解析当前令牌
        String token = resolveToken(request);
        if (StringUtils.hasText(token)) {
            Claims claims = jwtTokenProvider.parseToken(token);
            adminAuthService.logout(claims.getId(), jwtTokenProvider.remainingSeconds(claims));
        }
        return ApiResult.ok();
    }

    /**
     * 获取当前登录用户信息
     *
     * @param request HTTP 请求
     * @return 用户信息
     */
    @GetMapping("/me")
    public ApiResult<AdminUserVO> me(HttpServletRequest request) {
        // 1. 从过滤器写入的上下文读取用户 ID
        Long userId = (Long) request.getAttribute(AuthConstants.ATTR_ADMIN_USER_ID);
        return ApiResult.ok(adminAuthService.getUserById(userId));
    }

    // 从 Authorization 头解析 Bearer Token
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(AuthConstants.HEADER_AUTHORIZATION);
        if (!StringUtils.hasText(header) || !header.startsWith(AuthConstants.BEARER_PREFIX)) {
            return null;
        }
        return header.substring(AuthConstants.BEARER_PREFIX.length()).trim();
    }
}
