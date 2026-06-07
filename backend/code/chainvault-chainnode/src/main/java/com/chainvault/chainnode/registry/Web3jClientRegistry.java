package com.chainvault.chainnode.registry;

import com.chainvault.chainnode.config.RpcHttpClientFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 动态 Web3j 客户端注册表（支持多 RPC 轮询）
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class Web3jClientRegistry {

    private final RpcHttpClientFactory rpcHttpClientFactory;

    private final AtomicReference<List<Web3j>> ethClients = new AtomicReference<>(Collections.emptyList());
    private final AtomicReference<List<Web3j>> bnbClients = new AtomicReference<>(Collections.emptyList());
    private final AtomicInteger ethIndex = new AtomicInteger(0);
    private final AtomicInteger bnbIndex = new AtomicInteger(0);

    /**
     * 获取 ETH Web3j 客户端（轮询）
     *
     * @return 客户端，未配置时返回 null
     */
    public Web3j getEth() {
        return pickClient(ethClients.get(), ethIndex, "ETH");
    }

    /**
     * 获取 BNB Web3j 客户端（轮询）
     *
     * @return 客户端，未配置时返回 null
     */
    public Web3j getBnb() {
        return pickClient(bnbClients.get(), bnbIndex, "BNB");
    }

    /**
     * 刷新 ETH RPC 客户端池
     *
     * @param rpcUrls RPC 地址列表
     */
    public void refreshEth(List<String> rpcUrls) {
        replaceClients(ethClients, ethIndex, rpcUrls, "ETH");
    }

    /**
     * 刷新 BNB RPC 客户端池
     *
     * @param rpcUrls RPC 地址列表
     */
    public void refreshBnb(List<String> rpcUrls) {
        replaceClients(bnbClients, bnbIndex, rpcUrls, "BNB");
    }

    // 轮询选取 Web3j 实例
    private Web3j pickClient(List<Web3j> clients, AtomicInteger index, String chain) {
        if (clients == null || clients.isEmpty()) {
            return null;
        }
        if (clients.size() == 1) {
            return clients.get(0);
        }
        int pos = Math.floorMod(index.getAndIncrement(), clients.size());
        return clients.get(pos);
    }

    // 替换 Web3j 客户端池
    private void replaceClients(AtomicReference<List<Web3j>> holder,
                                  AtomicInteger index,
                                  List<String> rpcUrls,
                                  String chain) {
        List<Web3j> old = holder.get();
        List<String> validUrls = new ArrayList<>();
        if (rpcUrls != null) {
            for (String rpcUrl : rpcUrls) {
                if (rpcUrl != null && !rpcUrl.isBlank()) {
                    validUrls.add(rpcUrl.trim());
                }
            }
        }

        if (validUrls.isEmpty()) {
            holder.set(Collections.emptyList());
            index.set(0);
            shutdownAll(old, chain);
            log.info("[{}] RPC 未配置，已停用 Web3j 客户端池", chain);
            return;
        }

        List<Web3j> created = new ArrayList<>();
        OkHttpClient okHttpClient = rpcHttpClientFactory.createOkHttpClient();
        for (String rpcUrl : validUrls) {
            created.add(Web3j.build(new HttpService(rpcUrl, okHttpClient, false)));
        }
        holder.set(Collections.unmodifiableList(created));
        index.set(0);
        shutdownAll(old, chain);
        log.info("[{}] Web3j 客户端池已刷新，共 {} 个端点", chain, created.size());
    }

    // 静默关闭旧 Web3j 池
    private void shutdownAll(List<Web3j> clients, String chain) {
        if (clients == null || clients.isEmpty()) {
            return;
        }
        for (Web3j web3j : clients) {
            shutdownQuietly(web3j, chain);
        }
    }

    // 静默关闭单个 Web3j
    private void shutdownQuietly(Web3j web3j, String chain) {
        if (web3j == null) {
            return;
        }
        try {
            web3j.shutdown();
        } catch (Exception e) {
            log.warn("[{}] 关闭旧 Web3j 客户端异常", chain, e);
        }
    }
}
