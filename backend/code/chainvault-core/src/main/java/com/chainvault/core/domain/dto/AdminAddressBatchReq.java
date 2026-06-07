package com.chainvault.core.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 运营后台批量生成充值地址请求
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class AdminAddressBatchReq {

    /** 商户号 */
    @NotBlank(message = "merchantId 不能为空")
    private String merchantId;

    /** 链标识 */
    @NotBlank(message = "chainCode 不能为空")
    private String chainCode;

    /** 显示符号 */
    @NotBlank(message = "symbol 不能为空")
    private String symbol;

    /** 生成数量 */
    @NotNull(message = "count 不能为空")
    @Min(value = 1, message = "count 最小为 1")
    @Max(value = 100, message = "count 最大为 100")
    private Integer count;
}
