package com.chainvault.core.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.chainvault.common.enums.AdminUserRole;
import com.chainvault.common.enums.AdminUserStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 运营后台用户实体
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
@TableName("admin_user")
public class AdminUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录用户名 */
    private String username;

    /** BCrypt 密码哈希 */
    private String password;

    /** 显示名称 */
    private String displayName;

    /**
     * 角色
     * @see AdminUserRole
     */
    private String role;

    /**
     * 用户状态
     * @see AdminUserStatus
     */
    private Integer status;

    /** 最近登录时间 */
    private LocalDateTime lastLoginAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
