package com.chainvault.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 运营后台用户状态枚举
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Getter
@RequiredArgsConstructor
public enum AdminUserStatus {

    /**
     * 已禁用，无法登录
     */
    DISABLED(0, "禁用"),

    /**
     * 正常，可登录
     */
    ACTIVE(1, "正常");

    /** 状态码 */
    private final int code;

    /** 状态描述 */
    private final String desc;
}
