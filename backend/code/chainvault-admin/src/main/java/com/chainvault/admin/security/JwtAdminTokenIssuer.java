package com.chainvault.admin.security;

import com.chainvault.common.constants.AuthConstants;
import com.chainvault.core.domain.entity.AdminUser;
import com.chainvault.core.domain.vo.AdminLoginVO;
import com.chainvault.core.domain.vo.AdminUserVO;
import com.chainvault.core.service.AdminTokenIssuer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * JWT 令牌签发实现
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Component
@RequiredArgsConstructor
public class JwtAdminTokenIssuer implements AdminTokenIssuer {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 签发登录响应
     *
     * @param user 已认证用户
     * @return 登录结果
     */
    @Override
    public AdminLoginVO issue(AdminUser user) {
        // 1. 生成 JWT
        String token = jwtTokenProvider.createToken(user);

        // 2. 组装用户信息
        AdminUserVO userVO = new AdminUserVO();
        userVO.setId(user.getId());
        userVO.setUsername(user.getUsername());
        userVO.setDisplayName(user.getDisplayName());
        userVO.setRole(user.getRole());

        // 3. 返回登录响应
        AdminLoginVO vo = new AdminLoginVO();
        vo.setToken(token);
        vo.setTokenType(AuthConstants.TOKEN_TYPE_BEARER);
        vo.setExpiresIn(jwtTokenProvider.getExpirationSeconds());
        vo.setUser(userVO);
        return vo;
    }
}
