package com.chainvault.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Core 模块配置
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
@Component
@ConfigurationProperties(prefix = "chainvault")
public class CoreProperties {

    /** 是否启用定时归集扫描 */
    private boolean sweepEnabled = true;

    /** 归集阈值倍数（阈值 = min_deposit × 倍数） */
    private int sweepThresholdMultiplier = 5;

    /** HTTP 回调超时毫秒 */
    private int webhookTimeoutMs = 10000;
}
