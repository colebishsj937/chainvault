package com.chainvault.core.domain.vo;

import lombok.Data;

/**
 * 运营后台登录响应
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class AdminLoginVO {

    /** JWT 访问令牌 */
    private String token;

    /** 令牌类型，固定 Bearer */
    private String tokenType;

    /** 有效期（秒） */
    private long expiresIn;

    /** 当前用户信息 */
    private AdminUserVO user;
}
