package com.chainvault.chainnode.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 链节点配置属性
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
@Component
@ConfigurationProperties(prefix = "chainvault")
public class ChainNodeProperties {

    /** 是否启用扫块 */
    private boolean scanEnabled = true;

    /** 单次扫块最大块数 */
    private int scanBatchSize = 50;

    /** 是否并行扫块（各链独立线程调度，互不阻塞） */
    private boolean scanParallelEnabled = true;

    /** 扫块线程池大小（建议 ≥ 已启用链数量） */
    private int scanParallelThreads = 4;

    /** 开发模式：模拟链上广播（不实际发交易） */
    private boolean broadcastSimulate = true;

    /** 链节点 RPC HTTP 代理（Infura / TronGrid 等外链访问） */
    private RpcProxy rpcProxy = new RpcProxy();

    private Eth eth = new Eth();
    private Bnb bnb = new Bnb();
    private Tron tron = new Tron();
    private Btc btc = new Btc();

    @Data
    public static class RpcProxy {
        /** 是否启用 HTTP 代理 */
        private boolean enabled = false;
        /** 代理主机 */
        private String host = "127.0.0.1";
        /** 代理端口 */
        private int port = 7890;
        /** 代理用户名（可选） */
        private String username;
        /** 代理密码（可选） */
        private String password;
    }

    @Data
    public static class Eth {
        private String rpcUrl;
        private int requiredConfirms = 12;
    }

    @Data
    public static class Bnb {
        private String rpcUrl;
        private int requiredConfirms = 15;
    }

    @Data
    public static class Tron {
        private String apiUrl = "https://api.trongrid.io";
        private String apiKey;
        private int requiredConfirms = 20;
    }

    @Data
    public static class Btc {
        private String rpcUrl;
        private String rpcUser;
        private String rpcPassword;
        private int requiredConfirms = 6;
    }
}
