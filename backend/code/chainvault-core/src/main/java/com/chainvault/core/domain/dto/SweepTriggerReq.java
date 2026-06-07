package com.chainvault.core.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 手动触发资金归集请求
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class SweepTriggerReq {

    /** 商户号 */
    @NotBlank(message = "merchantId 不能为空")
    private String merchantId;

    /** 币种（可选，不传则扫描该商户全部币种） */
    private String coinType;
}
