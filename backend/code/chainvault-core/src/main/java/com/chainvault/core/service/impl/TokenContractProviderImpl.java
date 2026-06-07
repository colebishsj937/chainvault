package com.chainvault.core.service.impl;

import com.chainvault.chainnode.service.TokenContractProvider;
import com.chainvault.core.service.CoinConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 代币合约地址提供者实现
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Service
@RequiredArgsConstructor
public class TokenContractProviderImpl implements TokenContractProvider {

    private final CoinConfigService coinConfigService;

    /**
     * 查询链上已配置的代币合约地址
     *
     * @param chainCode 链标识
     * @return 合约地址列表
     */
    @Override
    public List<String> listContracts(String chainCode) {
        return coinConfigService.listTokenContractsByChain(chainCode);
    }
}
