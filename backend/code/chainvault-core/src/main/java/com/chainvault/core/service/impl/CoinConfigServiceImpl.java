package com.chainvault.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chainvault.core.domain.entity.CoinConfig;
import com.chainvault.core.mapper.CoinConfigMapper;
import com.chainvault.core.service.CoinConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 币种配置业务实现
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Service
@RequiredArgsConstructor
public class CoinConfigServiceImpl implements CoinConfigService {

    private final CoinConfigMapper coinConfigMapper;

    /**
     * 查询开源版可用币种
     *
     * @return 币种列表
     */
    @Transactional(readOnly = true)
    @Override
    public List<CoinConfig> listOpenCoins() {
        return coinConfigMapper.selectList(
                new LambdaQueryWrapper<CoinConfig>()
                        .eq(CoinConfig::getIsEnabled, 1)
                        .eq(CoinConfig::getIsOpen, 1));
    }

    /**
     * 按币种标识查询
     *
     * @param coinType 币种标识
     * @return 币种配置
     */
    @Transactional(readOnly = true)
    @Override
    public CoinConfig getByCoinType(String coinType) {
        return coinConfigMapper.selectOne(
                new LambdaQueryWrapper<CoinConfig>().eq(CoinConfig::getCoinType, coinType));
    }

    /**
     * 按链与合约地址查询代币配置
     *
     * @param chainCode    链标识
     * @param contractAddr 合约地址
     * @return 币种配置
     */
    @Transactional(readOnly = true)
    @Override
    public CoinConfig getByChainAndContract(String chainCode, String contractAddr) {
        if (!StringUtils.hasText(contractAddr)) {
            return null;
        }
        if (contractAddr.startsWith("0x")) {
            return coinConfigMapper.selectOne(
                    new LambdaQueryWrapper<CoinConfig>()
                            .eq(CoinConfig::getChainCode, chainCode)
                            .apply("LOWER(contract_addr) = {0}", contractAddr.toLowerCase()));
        }
        return coinConfigMapper.selectOne(
                new LambdaQueryWrapper<CoinConfig>()
                        .eq(CoinConfig::getChainCode, chainCode)
                        .eq(CoinConfig::getContractAddr, contractAddr));
    }

    /**
     * 查询链上已配置的代币合约地址列表
     *
     * @param chainCode 链标识
     * @return 合约地址列表
     */
    @Transactional(readOnly = true)
    @Override
    public CoinConfig getByChainAndSymbol(String chainCode, String symbol) {
        if (!StringUtils.hasText(chainCode) || !StringUtils.hasText(symbol)) {
            return null;
        }
        return coinConfigMapper.selectOne(
                new LambdaQueryWrapper<CoinConfig>()
                        .eq(CoinConfig::getChainCode, chainCode)
                        .eq(CoinConfig::getSymbol, symbol)
                        .eq(CoinConfig::getIsEnabled, 1)
                        .last("LIMIT 1"));
    }

    /**
     * 查询链上已配置的代币合约地址列表
     *
     * @param chainCode 链标识
     * @return 合约地址列表
     */
    @Transactional(readOnly = true)
    @Override
    public List<String> listTokenContractsByChain(String chainCode) {
        return coinConfigMapper.selectList(
                        new LambdaQueryWrapper<CoinConfig>()
                                .eq(CoinConfig::getChainCode, chainCode)
                                .eq(CoinConfig::getIsEnabled, 1)
                                .isNotNull(CoinConfig::getContractAddr)
                                .ne(CoinConfig::getContractAddr, ""))
                .stream()
                .map(CoinConfig::getContractAddr)
                .toList();
    }
}
