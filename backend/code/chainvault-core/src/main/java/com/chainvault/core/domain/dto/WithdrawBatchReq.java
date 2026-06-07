package com.chainvault.core.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 批量提币请求
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class WithdrawBatchReq {

    /** 商户号 */
    @NotBlank(message = "merchantId 不能为空")
    private String merchantId;

    /** 提币明细，最多 50 笔 */
    @NotEmpty(message = "items 不能为空")
    @Size(max = 50, message = "批量提币最多 50 笔")
    @Valid
    private List<WithdrawBatchItem> items;
}
