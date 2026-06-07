package com.chainvault.core.service.impl;

import com.chainvault.chainnode.dto.ChainNodeSettings;
import com.chainvault.chainnode.registry.Web3jClientRegistry;
import com.chainvault.chainnode.service.ChainNodeSettingsProvider;
import com.chainvault.common.constants.ChainCode;
import com.chainvault.core.service.ChainNodeConfigService;
import com.chainvault.core.util.ChainNodeUrlBuilder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 链节点运行时配置提供者实现
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChainNodeSettingsProviderImpl implements ChainNodeSettingsProvider {

    private final ChainNodeConfigService chainNodeConfigService;
    private final Web3jClientRegistry web3jClientRegistry;

    private final Map<String, ChainNodeSettings> cache = new ConcurrentHashMap<>();

    /**
     * 启动时加载配置
     */
    @PostConstruct
    public void init() {
        refreshAll();
    }

    /**
     * 获取链配置
     *
     * @param chainCode 链标识
     * @return 配置
     */
    @Override
    public Optional<ChainNodeSettings> getSettings(String chainCode) {
        return Optional.ofNullable(cache.get(chainCode.toUpperCase()));
    }

    /**
     * 刷新全部链配置缓存
     */
    @Override
    public void refreshAll() {
        // 1. 逐链解析有效配置
        cache.clear();
        loadChain(ChainCode.ETH);
        loadChain(ChainCode.BNB);
        loadChain(ChainCode.TRON);
        loadChain(ChainCode.BTC);

        // 2. 刷新 Web3j 客户端
        ChainNodeSettings eth = cache.get(ChainCode.ETH);
        ChainNodeSettings bnb = cache.get(ChainCode.BNB);
        web3jClientRegistry.refreshEth(eth != null && eth.isEnabled()
                ? ChainNodeUrlBuilder.buildRpcUrls(eth) : java.util.List.of());
        web3jClientRegistry.refreshBnb(bnb != null && bnb.isEnabled()
                ? ChainNodeUrlBuilder.buildRpcUrls(bnb) : java.util.List.of());
        log.info("链节点运行时配置已刷新");
    }

    // 加载单链配置到缓存
    private void loadChain(String chainCode) {
        chainNodeConfigService.resolveRuntimeSettings(chainCode).ifPresent(settings -> {
            cache.put(chainCode, settings);
        });
    }
}
