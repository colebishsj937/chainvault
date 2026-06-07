package com.chainvault.common.constants;

/**
 * 运营后台 JWT 鉴权常量
 *
 * @author chainvault
 * @date 2026-06-05
 */
public final class AuthConstants {

    private AuthConstants() {
    }

    /** Authorization 请求头 */
    public static final String HEADER_AUTHORIZATION = "Authorization";

    /** Bearer 前缀 */
    public static final String BEARER_PREFIX = "Bearer ";

    /** Token 类型 */
    public static final String TOKEN_TYPE_BEARER = "Bearer";

    /** JWT 黑名单 Redis 前缀 */
    public static final String JWT_BLACKLIST_PREFIX = "cv:admin:jwt:blacklist:";

    /** 请求属性：当前登录用户 ID */
    public static final String ATTR_ADMIN_USER_ID = "adminUserId";

    /** 请求属性：当前登录用户名 */
    public static final String ATTR_ADMIN_USERNAME = "adminUsername";

    /** JWT claim：用户 ID */
    public static final String CLAIM_USER_ID = "uid";

    /** JWT claim：用户名 */
    public static final String CLAIM_USERNAME = "username";

    /** JWT claim：显示名称 */
    public static final String CLAIM_DISPLAY_NAME = "displayName";

    /** JWT claim：角色 */
    public static final String CLAIM_ROLE = "role";

    /** JWT claim：令牌唯一 ID */
    public static final String CLAIM_JTI = "jti";
}
