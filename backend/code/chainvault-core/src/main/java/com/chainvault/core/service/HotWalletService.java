package com.chainvault.core.service;

import com.chainvault.core.domain.vo.BalanceVO;

import java.math.BigDecimal;

/**
 * 热钱包余额业务接口
 *
 * @author chainvault
 * @date 2026-06-05
 */
public interface HotWalletService {

    /**
     * 增加可用余额
     *
     * @param merchantId 商户号
     * @param coinType   币种标识
     * @param amount     增加金额
     */
    void addBalance(String merchantId, String coinType, BigDecimal amount);

    /**
     * 冻结可用余额（提币申请）
     *
     * @param merchantId 商户号
     * @param coinType   币种标识
     * @param amount     冻结金额
     */
    void freezeBalance(String merchantId, String coinType, BigDecimal amount);

    /**
     * 解冻失败提币的冻结金额
     *
     * @param merchantId 商户号
     * @param coinType   币种标识
     * @param amount     解冻金额
     */
    void unfreezeBalance(String merchantId, String coinType, BigDecimal amount);

    /**
     * 提币成功后扣减冻结余额
     *
     * @param merchantId 商户号
     * @param coinType   币种标识
     * @param amount     扣减金额
     */
    void commitFrozen(String merchantId, String coinType, BigDecimal amount);

    /**
     * 查询商户币种余额
     *
     * @param merchantId 商户号
     * @param coinType   币种标识
     * @return 余额视图
     */
    BalanceVO getBalance(String merchantId, String coinType);
}
