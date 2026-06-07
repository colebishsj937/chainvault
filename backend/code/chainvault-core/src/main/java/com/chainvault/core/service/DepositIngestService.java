package com.chainvault.core.service;

/**
 * 充值入账业务接口
 *
 * @author chainvault
 * @date 2026-06-05
 */
public interface DepositIngestService {

    /**
     * 扫描所有已启用链的新区块并入账
     */
    void scanAllChains();
}
