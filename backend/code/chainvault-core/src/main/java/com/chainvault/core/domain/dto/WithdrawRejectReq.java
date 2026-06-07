package com.chainvault.core.domain.dto;

import lombok.Data;

/**
 * 提币拒绝请求
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class WithdrawRejectReq {

    /** 拒绝原因 */
    private String reason;
}
