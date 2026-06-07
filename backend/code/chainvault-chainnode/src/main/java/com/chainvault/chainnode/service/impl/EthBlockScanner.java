package com.chainvault.chainnode.service.impl;

import com.chainvault.chainnode.dto.ChainNodeSettings;
import com.chainvault.chainnode.registry.Web3jClientRegistry;
import com.chainvault.chainnode.service.AbstractEvmBlockScanner;
import com.chainvault.chainnode.service.ChainNodeSettingsProvider;
import com.chainvault.chainnode.service.TokenContractProvider;
import com.chainvault.common.constants.ChainCode;
import org.springframework.stereotype.Component;

/**
 * ETH 链区块扫描器
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Component
public class EthBlockScanner extends AbstractEvmBlockScanner {

    /**
     * 构造 ETH 扫描器
     *
     * @param web3jClientRegistry      Web3j 注册表
     * @param chainNodeSettingsProvider 节点配置提供者
     * @param tokenContractProvider    代币合约提供者
     */
    public EthBlockScanner(Web3jClientRegistry web3jClientRegistry,
                           ChainNodeSettingsProvider chainNodeSettingsProvider,
                           TokenContractProvider tokenContractProvider) {
        super(web3jClientRegistry::getEth,
                ChainCode.ETH,
                "ETH",
                () -> chainNodeSettingsProvider.getSettings(ChainCode.ETH)
                        .map(ChainNodeSettings::getRequiredConfirms)
                        .orElse(12),
                tokenContractProvider);
    }
}
