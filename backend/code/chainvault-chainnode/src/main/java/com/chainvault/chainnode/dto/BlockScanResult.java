package com.chainvault.chainnode.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 区块扫描结果
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class BlockScanResult {

    /** 扫描到的最后区块高度 */
    private long lastScannedBlock;

    /** 转账事件列表 */
    private List<ChainTransferEvent> events = new ArrayList<>();
}
