package com.chainvault.core.service;

import com.chainvault.core.domain.entity.CoinConfig;

import java.util.List;

/**
 * 币种配置业务接口
 *
 * @author chainvault
 * @date 2026-06-05
 */
public interface CoinConfigService {

    /**
     * 查询开源版可用币种列表
     *
     * @return 币种配置列表
     */
    List<CoinConfig> listOpenCoins();

    /**
     * 按币种标识查询配置
     *
     * @param coinType 币种标识
     * @return 币种配置，不存在返回 null
     */
    CoinConfig getByCoinType(String coinType);

    /**
     * 按链与合约地址查询代币配置
     *
     * @param chainCode     链标识
     * @param contractAddr  合约地址
     * @return 币种配置，不存在返回 null
     */
    CoinConfig getByChainAndContract(String chainCode, String contractAddr);

    /**
     * 查询链上已配置的代币合约地址列表
     *
     * @param chainCode 链标识
     * @return 合约地址列表
     */
    java.util.List<String> listTokenContractsByChain(String chainCode);

    /**
     * 按链与显示符号查询币种配置
     *
     * @param chainCode 链标识
     * @param symbol    显示符号
     * @return 币种配置，不存在返回 null
     */
    CoinConfig getByChainAndSymbol(String chainCode, String symbol);
}
