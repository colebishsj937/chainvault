package com.chainvault.common.constants;

/**
 * Webhook 投递常量
 *
 * @author chainvault
 * @date 2026-06-05
 */
public final class WebhookConstants {

    /** 全局最大重试次数 */
    public static final int MAX_RETRY = 5;

    /** 重试延迟队列（ZSET，score=下次投递时间戳秒） */
    public static final String RETRY_QUEUE = "cv:queue:webhook:retry";

    /**
     * @deprecated 已归集金额改由 sweep_record 表汇总，不再读写 Redis
     */
    @Deprecated
    public static final String SWEPT_KEY_PREFIX = "cv:sweep:swept:";

    private WebhookConstants() {
    }
}
