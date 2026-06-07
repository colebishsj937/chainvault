package com.chainvault.core.util;

import com.chainvault.chainnode.dto.ChainNodeSettings;
import com.chainvault.common.constants.ChainCode;
import com.chainvault.common.enums.ChainNodeProvider;
import com.chainvault.common.exception.BusinessException;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 链节点 RPC URL 构建工具
 *
 * @author chainvault
 * @date 2026-06-05
 */
public final class ChainNodeUrlBuilder {

    private ChainNodeUrlBuilder() {
    }

    /**
     * 根据服务商与链标识构建有效 RPC URL
     *
     * @param chainCode 链标识
     * @param provider  服务商
     * @param apiKey    API Key
     * @param rpcUrl    自定义 RPC
     * @return 有效 RPC URL
     */
    public static String buildRpcUrl(String chainCode, ChainNodeProvider provider, String apiKey, String rpcUrl) {
        // 1. 自定义模式直接返回完整地址
        if (provider == ChainNodeProvider.CUSTOM) {
            if (!StringUtils.hasText(rpcUrl)) {
                return null;
            }
            return rpcUrl.trim();
        }

        // 2. Bitcoin Core 使用配置的 RPC 地址
        if (provider == ChainNodeProvider.BITCOIN_CORE) {
            if (!StringUtils.hasText(rpcUrl)) {
                return null;
            }
            return rpcUrl.trim();
        }

        // 3. TronGrid 使用 HTTP API（apiUrl），无 RPC URL
        if (provider == ChainNodeProvider.TRONGRID) {
            return null;
        }

        // 4. Alchemy / Infura 需 API Key
        if (!StringUtils.hasText(apiKey)) {
            return null;
        }

        String key = apiKey.trim();
        if (provider == ChainNodeProvider.ALCHEMY) {
            return buildAlchemyUrl(chainCode, key);
        }
        if (provider == ChainNodeProvider.INFURA) {
            return buildInfuraUrl(chainCode, key);
        }

        throw new BusinessException(400, "链 " + chainCode + " 不支持服务商 " + provider.getCode());
    }

    /**
     * 根据运行时配置构建全部有效 RPC URL（多 Key 轮询）
     *
     * @param settings 运行时配置
     * @return RPC URL 列表
     */
    public static List<String> buildRpcUrls(ChainNodeSettings settings) {
        if (settings == null) {
            return List.of();
        }

        // 1. 解析 API Key 列表
        List<String> apiKeys = new ArrayList<>();
        if (settings.getApiKeys() != null) {
            for (String key : settings.getApiKeys()) {
                if (StringUtils.hasText(key)) {
                    apiKeys.add(key.trim());
                }
            }
        }
        if (apiKeys.isEmpty() && StringUtils.hasText(settings.getApiKey())) {
            apiKeys.add(settings.getApiKey().trim());
        }

        ChainNodeProvider provider = ChainNodeProvider.fromCode(settings.getProvider());

        // 2. TronGrid 无 RPC 端点
        if (provider == ChainNodeProvider.TRONGRID) {
            return List.of();
        }

        // 3. 自定义 / BTC 仅单端点
        if (provider == ChainNodeProvider.CUSTOM || provider == ChainNodeProvider.BITCOIN_CORE) {
            String url = buildRpcUrl(settings.getChainCode(), provider, null, settings.getRpcUrl());
            if (StringUtils.hasText(url)) {
                return List.of(url);
            }
            return List.of();
        }

        // 4. Alchemy / Infura 按 Key 数量生成多个端点
        List<String> urls = new ArrayList<>();
        for (String apiKey : apiKeys) {
            String url = buildRpcUrl(settings.getChainCode(), provider, apiKey, settings.getRpcUrl());
            if (StringUtils.hasText(url)) {
                urls.add(url);
            }
        }
        return urls;
    }

    // 构建 Alchemy RPC URL
    private static String buildAlchemyUrl(String chainCode, String apiKey) {
        if (ChainCode.ETH.equalsIgnoreCase(chainCode)) {
            return "https://eth-mainnet.g.alchemy.com/v2/" + apiKey;
        }
        if (ChainCode.BNB.equalsIgnoreCase(chainCode)) {
            return "https://bnb-mainnet.g.alchemy.com/v2/" + apiKey;
        }
        throw new BusinessException(400, "Alchemy 暂不支持链: " + chainCode);
    }

    // 构建 Infura RPC URL
    private static String buildInfuraUrl(String chainCode, String apiKey) {
        if (ChainCode.ETH.equalsIgnoreCase(chainCode)) {
            return "https://mainnet.infura.io/v3/" + apiKey;
        }
        if (ChainCode.BNB.equalsIgnoreCase(chainCode)) {
            return "https://bsc-mainnet.infura.io/v3/" + apiKey;
        }
        throw new BusinessException(400, "Infura 暂不支持链: " + chainCode);
    }
}
