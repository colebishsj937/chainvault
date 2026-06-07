package com.chainvault.core.config;

import com.chainvault.chainnode.config.ChainNodeProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * 各链独立扫块线程池配置
 *
 * @author chainvault
 * @date 2026-06-07
 */
@Configuration
@Slf4j
public class ChainScanExecutorConfig {

    /**
     * 创建链扫块专用线程池，每条链在独立线程中执行，避免 ETH 长批次阻塞 TRON/BNB
     *
     * @param properties 链节点配置
     * @return 扫块线程池
     */
    @Bean(name = "chainScanExecutor", destroyMethod = "shutdown")
    public ExecutorService chainScanExecutor(ChainNodeProperties properties) {
        // 1. 计算线程池大小
        int poolSize = Math.max(1, properties.getScanParallelThreads());
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("chain-scan-" + thread.threadId());
            thread.setDaemon(true);
            return thread;
        };

        log.info("链扫块线程池已初始化 poolSize={} parallelEnabled={}",
                poolSize, properties.isScanParallelEnabled());
        return Executors.newFixedThreadPool(poolSize, threadFactory);
    }
}
