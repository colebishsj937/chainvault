package com.chainvault.core.domain.vo;

import lombok.Data;

/**
 * 归集全局配置视图
 *
 * @author chainvault
 * @date 2026-06-07
 */
@Data
public class SweepConfigVO {

    /** 是否启用定时归集扫描：0=否 1=是 */
    private Integer sweepEnabled;

    /** 归集阈值倍数 */
    private Integer thresholdMultiplier;

    /** 阈值计算公式说明 */
    private String thresholdFormula;

    /** 更新时间 */
    private String updatedAt;
}
