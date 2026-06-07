package com.chainvault.core.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 归集全局配置更新请求
 *
 * @author chainvault
 * @date 2026-06-07
 */
@Data
public class SweepConfigUpdateReq {

    /**
     * 是否启用定时归集扫描：0=否 1=是
     */
    @NotNull(message = "定时归集开关不能为空")
    @Min(value = 0, message = "定时归集开关取值无效")
    @Max(value = 1, message = "定时归集开关取值无效")
    private Integer sweepEnabled;

    /**
     * 归集阈值倍数
     */
    @NotNull(message = "阈值倍数不能为空")
    @Min(value = 1, message = "阈值倍数至少为 1")
    @Max(value = 100, message = "阈值倍数不能超过 100")
    private Integer thresholdMultiplier;
}
