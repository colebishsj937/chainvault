package com.chainvault.chainnode.util;

import com.chainvault.chainnode.dto.ChainNodeSettings;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 链节点 API Key 轮询器（请求级轮换，分散单 Key 额度）
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Component
public class ChainNodeApiKeyRotator {

    private final ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    /**
     * 获取下一次请求应使用的 API Key
     *
     * @param settings 链运行时配置
     * @return API Key，无可用 Key 时返回 null
     */
    public String nextApiKey(ChainNodeSettings settings) {
        if (settings == null) {
            return null;
        }

        // 1. 解析可用 Key 列表
        List<String> keys = resolveApiKeys(settings);
        if (keys.isEmpty()) {
            return null;
        }
        if (keys.size() == 1) {
            return keys.get(0);
        }

        // 2. 按链轮询选取
        String chainCode = settings.getChainCode().toUpperCase();
        AtomicInteger counter = counters.computeIfAbsent(chainCode, ignored -> new AtomicInteger(0));
        int index = Math.floorMod(counter.getAndIncrement(), keys.size());
        return keys.get(index);
    }

    // 解析有效 API Key 列表
    private List<String> resolveApiKeys(ChainNodeSettings settings) {
        List<String> keys = new ArrayList<>();
        if (settings.getApiKeys() != null) {
            for (String key : settings.getApiKeys()) {
                if (key != null && !key.isBlank()) {
                    keys.add(key.trim());
                }
            }
        }
        if (!keys.isEmpty()) {
            return keys;
        }
        if (settings.getApiKey() != null && !settings.getApiKey().isBlank()) {
            keys.add(settings.getApiKey().trim());
        }
        return keys;
    }
}
