package com.chainvault.chainnode.service;

import java.util.List;

/**
 * 代币合约地址提供者（由 core 模块实现）
 *
 * @author chainvault
 * @date 2026-06-05
 */
public interface TokenContractProvider {

    /**
     * 查询链上已配置的代币合约地址
     *
     * @param chainCode 链标识
     * @return 合约地址列表
     */
    List<String> listContracts(String chainCode);
}
