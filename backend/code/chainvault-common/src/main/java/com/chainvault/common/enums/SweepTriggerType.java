package com.chainvault.common.enums;

import lombok.Getter;

/**
 * 归集批次触发方式枚举
 */
@Getter
public enum SweepTriggerType {

    /**
     * Gateway 定时任务
     */
    SCHEDULED(1, "定时扫描"),

    /**
     * Admin 热钱包手动触发
     */
    ADMIN_MANUAL(2, "Admin手动"),

    /**
     * Admin 对失败批次重试
     */
    ADMIN_RETRY_BATCH(3, "Admin批次重试"),

    /**
     * Gateway 商户 API
     */
    MERCHANT_API(4, "商户API");

    /** 类型码 */
    private final int code;

    /** 描述 */
    private final String desc;

    SweepTriggerType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
