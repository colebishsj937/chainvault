package com.chainvault.chainnode.service.impl;

import com.chainvault.chainnode.service.BlockScanner;
import com.chainvault.chainnode.service.ChainHealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 链节点健康检查实现
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Service
@RequiredArgsConstructor
public class ChainHealthServiceImpl implements ChainHealthService {

    private final List<BlockScanner> blockScanners;

    /**
     * 返回各链节点状态
     *
     * @return 链状态映射
     */
    @Override
    public Map<String, String> checkAll() {
        Map<String, String> status = new LinkedHashMap<>();

        // 1. 遍历扫描器探测连通性
        for (BlockScanner scanner : blockScanners) {
            if (!scanner.enabled()) {
                status.put(scanner.chainCode(), "NOT_CONFIGURED");
                continue;
            }
            try {
                scanner.latestBlockNumber();
                status.put(scanner.chainCode(), "UP");
            } catch (Exception ex) {
                status.put(scanner.chainCode(), "DOWN");
            }
        }
        return status;
    }
}
