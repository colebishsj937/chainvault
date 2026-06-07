package com.chainvault.core.domain.vo;

import lombok.Data;

/**
 * 热钱包余额汇总视图（运营后台）
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class WalletBalanceVO {

    /** 链标识 */
    private String chainCode;

    /** 内部币种标识，如 USDT_ETH */
    private String coinType;

    /** 显示符号 */
    private String symbol;

    /** 全平台可用余额合计 */
    private String balance;

    /** 全平台冻结余额合计 */
    private String frozenBalance;
}
