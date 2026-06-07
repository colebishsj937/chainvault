package com.chainvault.core.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 热钱包余额视图
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class BalanceVO {

    /** 商户号 */
    private String merchantId;

    /** 币种 */
    private String coinType;

    /** 可用余额 */
    private BigDecimal balance;

    /** 冻结余额 */
    private BigDecimal frozen;
}
