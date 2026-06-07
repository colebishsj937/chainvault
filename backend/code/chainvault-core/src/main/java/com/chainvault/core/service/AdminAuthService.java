package com.chainvault.core.service;

import com.chainvault.core.domain.dto.AdminLoginReq;
import com.chainvault.core.domain.vo.AdminLoginVO;
import com.chainvault.core.domain.vo.AdminUserVO;

/**
 * 运营后台认证服务
 *
 * @author chainvault
 * @date 2026-06-05
 */
public interface AdminAuthService {

    /**
     * 用户名密码登录，签发 JWT
     *
     * @param req 登录请求
     * @return 令牌与用户信息
     */
    AdminLoginVO login(AdminLoginReq req);

    /**
     * 登出，将当前令牌加入黑名单
     *
     * @param jti           令牌唯一 ID
     * @param ttlSeconds    黑名单保留秒数（与令牌剩余有效期一致）
     */
    void logout(String jti, long ttlSeconds);

    /**
     * 判断令牌是否已登出（黑名单）
     *
     * @param jti 令牌唯一 ID
     * @return true=已失效
     */
    boolean isTokenBlacklisted(String jti);

    /**
     * 根据用户 ID 查询当前用户信息
     *
     * @param userId 用户 ID
     * @return 用户视图
     */
    AdminUserVO getUserById(Long userId);
}
