package com.chainvault.core.service;

import com.chainvault.core.domain.vo.SweepTriggerVO;
import com.chainvault.core.domain.vo.WalletBalanceVO;

import java.util.List;

/**
 * 运营后台热钱包业务接口
 *
 * @author chainvault
 * @date 2026-06-05
 */
public interface AdminWalletService {

    /**
     * 查询热钱包余额汇总
     *
     * @param merchantId 商户号，为空时汇总全平台
     * @return 按链与币种聚合的余额列表
     */
    List<WalletBalanceVO> listBalances(String merchantId);

    /**
     * 触发指定链归集扫描
     *
     * @param chainCode  链标识
     * @param merchantId 商户号，可为空表示全平台
     * @param coinType   币种标识，可为空表示该链全部币种
     * @return 扫描统计
     */
    SweepTriggerVO collect(String chainCode, String merchantId, String coinType);
}
