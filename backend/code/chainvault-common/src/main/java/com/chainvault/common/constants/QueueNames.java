package com.chainvault.common.constants;

/**
 * Redis 队列名称常量
 *
 * @author chainvault
 * @date 2026-06-05
 */
public final class QueueNames {

    /** Webhook 投递队列 */
    public static final String WEBHOOK = "cv:queue:webhook";

    /** 提币广播队列 */
    public static final String WITHDRAW_BROADCAST = "cv:queue:withdraw";

    /** 资金归集队列 */
    public static final String SWEEP = "cv:queue:sweep";

    private QueueNames() {
    }
}
