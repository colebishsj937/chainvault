package com.chainvault.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 运营后台用户角色枚举
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Getter
@RequiredArgsConstructor
public enum AdminUserRole {

    /**
     * 系统管理员，拥有全部后台权限
     */
    ADMIN("ADMIN", "管理员"),

    /**
     * 运营人员，日常操作权限
     */
    OPERATOR("OPERATOR", "运营");

    /** 角色编码 */
    private final String code;

    /** 角色描述 */
    private final String desc;
}
