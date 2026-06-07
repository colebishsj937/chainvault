package com.chainvault.core.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 运营后台登录请求
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class AdminLoginReq {

    /** 用户名 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 密码 */
    @NotBlank(message = "密码不能为空")
    private String password;
}
