package com.chainvault.core.domain.vo;

import lombok.Data;

/**
 * 运营后台用户视图（不含密码）
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class AdminUserVO {

    /** 用户 ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 显示名称 */
    private String displayName;

    /**
     * 角色
     * @see com.chainvault.common.enums.AdminUserRole
     */
    private String role;
}
