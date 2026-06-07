package com.chainvault.chainnode.service;

import com.chainvault.chainnode.dto.BlockScanResult;

/**
 * 区块扫描器接口
 *
 * @author chainvault
 * @date 2026-06-05
 */
public interface BlockScanner {

    /**
     * 链标识
     *
     * @return 链标识
     */
    String chainCode();

    /**
     * 是否已配置并可扫描
     *
     * @return 是否启用
     */
    boolean enabled();

    /**
     * 所需确认数
     *
     * @return 确认数
     */
    int requiredConfirms();

    /**
     * 获取最新区块高度
     *
     * @return 最新区块高度
     */
    long latestBlockNumber();

    /**
     * 扫描区块区间
     *
     * @param fromBlock 起始区块（含）
     * @param toBlock   结束区块（含）
     * @return 扫描结果
     */
    BlockScanResult scanRange(long fromBlock, long toBlock);
}
