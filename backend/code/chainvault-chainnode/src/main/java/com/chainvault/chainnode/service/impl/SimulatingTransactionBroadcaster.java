package com.chainvault.chainnode.service.impl;

import com.chainvault.chainnode.config.ChainNodeProperties;
import com.chainvault.chainnode.dto.BroadcastRequest;
import com.chainvault.chainnode.dto.BroadcastResult;
import com.chainvault.chainnode.service.TransactionBroadcaster;
import com.chainvault.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * 模拟链上广播（开发环境）
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SimulatingTransactionBroadcaster implements TransactionBroadcaster {

    private final ChainNodeProperties chainNodeProperties;

    /**
     * 模拟签名并广播提币交易
     *
     * @param request 广播请求
     * @return 模拟广播结果
     */
    @Override
    public BroadcastResult broadcast(BroadcastRequest request) {
        if (!chainNodeProperties.isBroadcastSimulate()) {
            throw new BusinessException("链节点广播未配置，请设置 chainvault.broadcast-simulate=true 或配置 RPC");
        }

        // 1. 生成确定性模拟 txHash
        String txHash = "sim_" + sha256Hex(request.getOrderNo() + ":" + request.getChainCode());

        // 2. 组装结果
        BroadcastResult result = new BroadcastResult();
        result.setTxHash(txHash);
        result.setFromAddress("hot-wallet@" + request.getChainCode().toLowerCase());
        result.setSimulated(true);

        log.info("[{}] 模拟提币广播 orderNo={} txHash={} amount={} {}",
                request.getChainCode(), request.getOrderNo(), txHash,
                request.getAmount(), request.getCoinType());
        return result;
    }

    // 计算 SHA256 十六进制摘要
    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new BusinessException("生成模拟 txHash 失败");
        }
    }
}
