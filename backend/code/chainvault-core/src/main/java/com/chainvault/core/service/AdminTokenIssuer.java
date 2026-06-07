package com.chainvault.core.service;

import com.chainvault.core.domain.entity.AdminUser;
import com.chainvault.core.domain.vo.AdminLoginVO;

/**
 * 运营后台 JWT 签发器（由 admin 模块实现）
 *
 * @author chainvault
 * @date 2026-06-05
 */
public interface AdminTokenIssuer {

    /**
     * 为指定用户签发访问令牌
     *
     * @param user 已认证用户
     * @return 登录响应（含 token）
     */
    AdminLoginVO issue(AdminUser user);
}
