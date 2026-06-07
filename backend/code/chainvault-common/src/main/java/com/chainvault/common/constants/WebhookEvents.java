package com.chainvault.common.constants;

/**
 * Webhook 事件类型常量
 *
 * @author chainvault
 * @date 2026-06-05
 */
public final class WebhookEvents {

    /** 充值待确认 */
    public static final String DEPOSIT_PENDING = "deposit.pending";

    /** 充值已确认 */
    public static final String DEPOSIT_CONFIRMED = "deposit.confirmed";

    /** 提币待广播 */
    public static final String WITHDRAW_PENDING = "withdraw.pending";

    /** 提币成功 */
    public static final String WITHDRAW_SUCCESS = "withdraw.success";

    /** 提币失败 */
    public static final String WITHDRAW_FAILED = "withdraw.failed";

    private WebhookEvents() {
    }
}
